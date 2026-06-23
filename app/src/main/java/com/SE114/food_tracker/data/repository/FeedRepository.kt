package com.SE114.food_tracker.data.repository

import android.content.Context
import android.net.Uri
import com.SE114.food_tracker.core.sync.SyncStatus
import com.SE114.food_tracker.data.local.dao.FeedCommentDto
import com.SE114.food_tracker.data.local.dao.FeedDAO
import com.SE114.food_tracker.data.local.dao.FeedPostDto
import com.SE114.food_tracker.data.local.dao.FeedSourceItemDto
import com.SE114.food_tracker.data.local.entities.FeedComment
import com.SE114.food_tracker.data.local.entities.FeedLike
import com.SE114.food_tracker.data.local.entities.FeedPost
import com.SE114.food_tracker.data.local.entities.UserProfileCacheEntity
import com.SE114.food_tracker.data.remote.dto.FeedCommentRemoteDTO
import com.SE114.food_tracker.data.remote.dto.FeedLikeRemoteDTO
import com.SE114.food_tracker.data.remote.dto.FeedPostRemoteDTO
import com.SE114.food_tracker.data.remote.dto.ProfileDTO
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val feedDao: FeedDAO,
    private val supabaseClient: SupabaseClient,
    @ApplicationContext private val context: Context
) {
    fun currentUserId(): String =
        supabaseClient.auth.currentUserOrNull()?.id ?: LOCAL_USER_ID

    fun currentDisplayName(): String =
        supabaseClient.auth.currentUserOrNull()?.email?.substringBefore("@") ?: "Thao Uyen"

    fun observePosts(pageSize: Int, page: Int): Flow<List<FeedPostDto>> =
        feedDao.observePosts(
            currentUserId = currentUserId(),
            limit = pageSize * page.coerceAtLeast(1),
            offset = 0
        )

    fun observeComments(postId: String): Flow<List<FeedCommentDto>> =
        feedDao.observeComments(postId)

    fun observeSourceItems(): Flow<List<FeedSourceItemDto>> =
        feedDao.observeSourceItems(limit = PAGE_SIZE)

    suspend fun refreshVisibleFromSupabase(): Boolean {
        val ownerId = currentAuthenticatedUserId()
        return pullVisibleFromSupabase(ownerId)
    }

    suspend fun createPostFromItem(
        item: FeedSourceItemDto,
        caption: String,
        visibility: String
    ) {
        createPost(
            itemId = item.itemId,
            imageUrl = item.imageUrl?.takeIf { it.isNotBlank() }
                ?: item.categoryIconUrl?.takeIf { it.isNotBlank() }?.let { "$EMOJI_IMAGE_PREFIX$it" }
                .orEmpty(),
            caption = caption.ifBlank { item.name },
            visibility = visibility
        )
    }

    suspend fun createPostFromImage(
        imageUrl: String,
        caption: String,
        visibility: String
    ) {
        createPost(
            itemId = null,
            imageUrl = imageUrl,
            caption = caption,
            visibility = visibility
        )
    }

    suspend fun toggleLike(postId: String) {
        feedDao.toggleLike(postId, currentUserId())
    }

    suspend fun addComment(postId: String, body: String) {
        val now = System.currentTimeMillis()
        feedDao.insertComment(
            FeedComment(
                postId = postId,
                userId = currentUserId(),
                displayName = currentDisplayName(),
                body = body.trim(),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun deletePost(postId: String): Boolean {
        val ownerId = currentAuthenticatedUserId()
        val rowsUpdated = feedDao.softDeletePost(postId, ownerId)
        if (rowsUpdated == 0) {
            error("Cannot delete this post with the current account.")
        }

        val deletedPost = feedDao.getPostById(postId)
            ?: throw FeedPostDeleteSyncException(
                "Đã ẩn bài viết trên máy, nhưng không tìm thấy bài viết local để đồng bộ lên Supabase."
            )

        return runCatching { pushPost(deletedPost, ownerId) }
            .onSuccess { feedDao.markPostSynced(postId) }
            .onFailure { throwable ->
                Timber.e(throwable, "[FeedSync] immediate post delete failed id=$postId")
                feedDao.markPostFailed(postId)
            }
            .getOrElse { throwable ->
                val reason = throwable.message ?: throwable::class.java.simpleName
                throw FeedPostDeleteSyncException(
                    message = "Đã ẩn bài viết trên máy, nhưng Supabase chưa cập nhật xóa: $reason",
                    cause = throwable
                )
            }
            .let { true }
    }

    suspend fun pushPendingToSupabase(ownerId: String): Boolean {
        var anyError = false

        for (post in feedDao.getPendingPosts()) {
            runCatching { pushPost(post, ownerId) }
                .onSuccess { feedDao.markPostSynced(post.postId) }
                .onFailure { throwable ->
                    Timber.e(throwable, "[FeedSync] post failed id=${post.postId}")
                    feedDao.markPostFailed(post.postId)
                    anyError = true
                }
        }

        for (like in feedDao.getPendingLikes()) {
            runCatching { pushLike(like, ownerId) }
                .onSuccess { feedDao.markLikeSynced(like.likeId) }
                .onFailure { throwable ->
                    Timber.e(throwable, "[FeedSync] like failed post=${like.postId}")
                    feedDao.markLikeFailed(like.likeId)
                    anyError = true
                }
        }

        for (comment in feedDao.getPendingComments()) {
            runCatching { pushComment(comment, ownerId) }
                .onSuccess { feedDao.markCommentSynced(comment.commentId) }
                .onFailure { throwable ->
                    Timber.e(throwable, "[FeedSync] comment failed id=${comment.commentId}")
                    feedDao.markCommentFailed(comment.commentId)
                    anyError = true
                }
        }

        return !anyError
    }

    suspend fun pullVisibleFromSupabase(ownerId: String): Boolean =
        runCatching {
            // Only client-readable columns (migration 0001); `select *` would hit the
            // ungranted sensitive columns (is_banned, …) and fail with a permission error.
            val profiles = supabaseClient.postgrest.from("profile")
                .select(Columns.list("id", "display_name", "user_id", "avatar_url"))
                .decodeList<ProfileDTO>()
                .associateBy { it.id }

            val profileEntities = profiles.values.map { it.toCacheEntity() }
            if (profileEntities.isNotEmpty()) {
                feedDao.insertUserProfiles(profileEntities)
            }

            val remotePosts = supabaseClient.postgrest.from("post")
                .select {
                    filter {
                        eq("is_deleted", false)
                    }
                }
                .decodeList<FeedPostRemoteDTO>()
            val locallyDeletedPostIds = feedDao.getDeletedPostIds().toSet()
            val visibleRemotePosts = remotePosts.filterNot { it.id in locallyDeletedPostIds }

            val postEntities = visibleRemotePosts.map { dto ->
                dto.toEntity(
                    ownerName = profiles[dto.authorId].displayNameOrFallback(dto.authorId)
                )
            }
            val remotePostIds = visibleRemotePosts.map { it.id }
            if (remotePostIds.isEmpty()) {
                feedDao.deleteAllSyncedPosts()
            } else {
                feedDao.deleteSyncedPostsMissingFromRemote(remotePostIds)
            }
            if (postEntities.isNotEmpty()) {
                feedDao.insertPosts(postEntities)
            }

            val remoteLikes = supabaseClient.postgrest.from("post_like")
                .select()
                .decodeList<FeedLikeRemoteDTO>()

            val likeEntities = remoteLikes.map { it.toEntity() }
            if (likeEntities.isNotEmpty()) {
                feedDao.insertLikes(likeEntities)
            }

            val remoteComments = supabaseClient.postgrest.from("post_comment")
                .select()
                .decodeList<FeedCommentRemoteDTO>()

            val commentEntities = remoteComments.map { dto ->
                dto.toEntity(
                    displayName = profiles[dto.authorId].displayNameOrFallback(dto.authorId)
                )
            }
            if (commentEntities.isNotEmpty()) {
                feedDao.insertComments(commentEntities)
            }

            Timber.d(
                "[FeedSync] pulled posts=${postEntities.size}, " +
                    "likes=${likeEntities.size}, comments=${commentEntities.size}, me=$ownerId"
            )
        }
            .onFailure { Timber.e(it, "[FeedSync] pull failed") }
            .isSuccess

    private suspend fun createPost(
        itemId: String?,
        imageUrl: String,
        caption: String,
        visibility: String
    ) {
        val now = System.currentTimeMillis()
        feedDao.insertPost(
            FeedPost(
                postId = UUID.randomUUID().toString(),
                ownerId = currentUserId(),
                ownerName = currentDisplayName(),
                itemId = itemId,
                imageUrl = imageUrl,
                caption = caption.trim(),
                visibility = visibility,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private suspend fun pushPost(post: FeedPost, ownerId: String) {
        val latestPost = feedDao.getPostById(post.postId) ?: post
        if (latestPost.ownerId != ownerId) {
            error("post ownerId ${latestPost.ownerId} does not match auth user $ownerId")
        }

        if (latestPost.isDeleted) {
            softDeleteRemotePost(latestPost, ownerId)
            return
        }

        val remoteImageUrl = uploadPostImageIfNeeded(latestPost, ownerId)
        val postBeforeUpsert = feedDao.getPostById(latestPost.postId) ?: latestPost
        if (postBeforeUpsert.isDeleted) {
            softDeleteRemotePost(postBeforeUpsert, ownerId)
            return
        }

        val dto = postBeforeUpsert.toRemoteDTO(ownerId, remoteImageUrl)
        supabaseClient.postgrest.from("post").upsert(dto)
    }

    private suspend fun softDeleteRemotePost(post: FeedPost, ownerId: String) {
        supabaseClient.postgrest.rpc(
            function = "soft_delete_post",
            parameters = SoftDeletePostRpcArgs(postId = post.postId)
        )
    }

    private suspend fun pushLike(like: FeedLike, ownerId: String) {
        if (like.userId != ownerId) {
            error("like userId ${like.userId} does not match auth user $ownerId")
        }

        if (like.isDeleted) {
            supabaseClient.postgrest.from("post_like").delete {
                filter {
                    eq("post_id", like.postId)
                    eq("user_id", ownerId)
                }
            }
        } else {
            supabaseClient.postgrest.from("post_like").upsert(like.toRemoteDTO(ownerId))
        }
    }

    private suspend fun pushComment(comment: FeedComment, ownerId: String) {
        if (comment.userId != ownerId) {
            error("comment userId ${comment.userId} does not match auth user $ownerId")
        }

        if (comment.isDeleted) {
            supabaseClient.postgrest.from("post_comment").delete {
                filter {
                    eq("id", comment.commentId)
                    eq("author_id", ownerId)
                }
            }
        } else {
            supabaseClient.postgrest.from("post_comment").upsert(comment.toRemoteDTO(ownerId))
        }
    }

    private suspend fun uploadPostImageIfNeeded(post: FeedPost, ownerId: String): String {
        if (
            post.imageUrl.isBlank() ||
            post.imageUrl.startsWith("http", ignoreCase = true) ||
            post.imageUrl.startsWith(EMOJI_IMAGE_PREFIX)
        ) {
            return post.imageUrl
        }

        val bytes = withContext(Dispatchers.IO) {
            val uri = Uri.parse(post.imageUrl)
            if (uri.scheme == "file") {
                val path = uri.path ?: error("cannot read local post image: ${post.imageUrl}")
                File(path).readBytes()
            } else {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("cannot read local post image: ${post.imageUrl}")
            }
        }

        val path = "$ownerId/${post.postId}.jpg"
        runCatching {
            supabaseClient.storage.from("posts").upload(path, bytes) {
                upsert = false
            }
        }.onFailure { throwable ->
            val message = throwable.message.orEmpty()
            val duplicate = message.contains("409") ||
                message.contains("already exists", ignoreCase = true) ||
                message.contains("duplicate", ignoreCase = true)

            if (!duplicate) throw throwable
        }

        val publicUrl = supabaseClient.storage.from("posts").publicUrl(path)
        feedDao.updatePostImageUrl(post.postId, publicUrl)
        return publicUrl
    }

    private fun FeedPost.toRemoteDTO(ownerId: String, remoteImageUrl: String): FeedPostRemoteDTO =
        FeedPostRemoteDTO(
            id = postId,
            authorId = ownerId,
            itemId = itemId,
            caption = caption.ifBlank { null },
            imageUrl = remoteImageUrl.ifBlank { null },
            visibility = visibility,
            isDeleted = false,
            createdAt = Instant.fromEpochMilliseconds(createdAt).toString(),
            deletedAt = null
        )

    private fun FeedLike.toRemoteDTO(ownerId: String): FeedLikeRemoteDTO =
        FeedLikeRemoteDTO(
            postId = postId,
            userId = ownerId,
            createdAt = Instant.fromEpochMilliseconds(createdAt).toString()
        )

    private fun FeedComment.toRemoteDTO(ownerId: String): FeedCommentRemoteDTO =
        FeedCommentRemoteDTO(
            id = commentId,
            postId = postId,
            authorId = ownerId,
            body = body,
            createdAt = Instant.fromEpochMilliseconds(createdAt).toString()
        )

    private fun FeedPostRemoteDTO.toEntity(ownerName: String): FeedPost =
        FeedPost(
            postId = id,
            ownerId = authorId,
            ownerName = ownerName,
            itemId = itemId,
            imageUrl = imageUrl.orEmpty(),
            caption = caption.orEmpty(),
            visibility = visibility,
            syncStatus = SyncStatus.SYNCED.name,
            isDeleted = isDeleted,
            createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
            updatedAt = Instant.parse(createdAt).toEpochMilliseconds()
        )

    private fun FeedLikeRemoteDTO.toEntity(): FeedLike =
        FeedLike(
            likeId = stableLikeId(postId, userId),
            postId = postId,
            userId = userId,
            syncStatus = SyncStatus.SYNCED.name,
            isDeleted = false,
            createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
            updatedAt = Instant.parse(createdAt).toEpochMilliseconds()
        )

    private fun FeedCommentRemoteDTO.toEntity(displayName: String): FeedComment =
        FeedComment(
            commentId = id,
            postId = postId,
            userId = authorId,
            displayName = displayName,
            body = body,
            syncStatus = SyncStatus.SYNCED.name,
            isDeleted = false,
            createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
            updatedAt = Instant.parse(createdAt).toEpochMilliseconds()
        )

    private fun ProfileDTO?.displayNameOrFallback(profileId: String): String =
        this?.displayName?.takeIf { it.isNotBlank() }
            ?: this?.userId?.takeIf { it.isNotBlank() }
            ?: profileId.take(8)

    private fun ProfileDTO.toCacheEntity(): UserProfileCacheEntity =
        UserProfileCacheEntity(
            userId = id,
            displayName = this.displayNameOrFallback(id),
            avatarUrl = avatarUrl
        )

    private suspend fun currentAuthenticatedUserId(): String {
        supabaseClient.auth.currentUserOrNull()?.id?.let { return it }

        val status = withTimeoutOrNull(AUTH_SESSION_WAIT_MS) {
            supabaseClient.auth.sessionStatus.first { it !is SessionStatus.Initializing }
        }

        if (status is SessionStatus.Authenticated) {
            status.session.user?.id?.let { return it }
            supabaseClient.auth.currentUserOrNull()?.id?.let { return it }
        }

        error("Not authenticated")
    }

    private fun stableLikeId(postId: String, userId: String): String =
        UUID.nameUUIDFromBytes("$postId:$userId".toByteArray()).toString()

    companion object {
        const val PAGE_SIZE = 30
        private const val EMOJI_IMAGE_PREFIX = "emoji:"
        private const val LOCAL_USER_ID = "local_user"
        private const val AUTH_SESSION_WAIT_MS = 2_000L
    }
}

@Serializable
private data class SoftDeletePostRpcArgs(
    @SerialName("p_post_id") val postId: String
)

class FeedPostDeleteSyncException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
