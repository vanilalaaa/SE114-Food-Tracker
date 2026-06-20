package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.data.local.dao.FeedCommentDto
import com.SE114.food_tracker.data.local.dao.FeedDAO
import com.SE114.food_tracker.data.local.dao.FeedPostDto
import com.SE114.food_tracker.data.local.dao.FeedSourceItemDto
import com.SE114.food_tracker.data.local.entities.FeedComment
import com.SE114.food_tracker.data.local.entities.FeedPost
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val feedDao: FeedDAO,
    private val supabaseClient: SupabaseClient
) {
    fun currentUserId(): String =
        supabaseClient.auth.currentUserOrNull()?.id ?: LOCAL_USER_ID

    fun currentDisplayName(): String =
        supabaseClient.auth.currentUserOrNull()?.email?.substringBefore("@") ?: "Thao Uyen"

    fun observePosts(pageSize: Int, page: Int): Flow<List<FeedPostDto>> =
        feedDao.observePosts(
            currentUserId = currentUserId(),
            limit = pageSize,
            offset = (page - 1).coerceAtLeast(0) * pageSize
        )

    fun observeComments(postId: String): Flow<List<FeedCommentDto>> =
        feedDao.observeComments(postId)

    fun observeSourceItems(): Flow<List<FeedSourceItemDto>> =
        feedDao.observeSourceItems(limit = PAGE_SIZE)

    suspend fun createPostFromItem(
        item: FeedSourceItemDto,
        caption: String,
        visibility: String
    ) {
        createPost(
            itemId = item.itemId,
            imageUrl = item.imageUrl.orEmpty(),
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

    companion object {
        const val PAGE_SIZE = 30
        private const val LOCAL_USER_ID = "local_user"
    }
}
