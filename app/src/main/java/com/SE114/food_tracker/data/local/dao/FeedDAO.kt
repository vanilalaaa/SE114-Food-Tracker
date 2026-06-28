package com.SE114.food_tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.SE114.food_tracker.data.local.entities.FeedComment
import com.SE114.food_tracker.data.local.entities.FeedHiddenPost
import com.SE114.food_tracker.data.local.entities.FeedLike
import com.SE114.food_tracker.data.local.entities.FeedPost
import com.SE114.food_tracker.data.local.entities.UserProfileCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDAO {

    @Query(
        """
        SELECT
            p.post_id AS postId,
            p.owner_id AS ownerId,
            COALESCE(u.display_name, p.owner_name) AS ownerName,
            u.avatar_url AS ownerAvatarUrl,
            p.item_id AS itemId,
            CASE
                WHEN p.item_id IS NULL AND instr(p.caption, char(10)) > 0
                    THEN trim(substr(p.caption, 1, instr(p.caption, char(10)) - 1))
                WHEN p.item_id IS NULL
                    THEN p.caption
                ELSE i.name
            END AS itemName,
            c.icon_url AS categoryIconUrl,
            p.image_url AS imageUrl,
            CASE
                WHEN p.item_id IS NULL AND instr(p.caption, char(10)) > 0
                    THEN trim(substr(p.caption, instr(p.caption, char(10)) + 1))
                WHEN p.item_id IS NULL
                    THEN ''
                ELSE p.caption
            END AS caption,
            p.visibility AS visibility,
            (
                SELECT COUNT(*)
                FROM feed_like l
                WHERE l.post_id = p.post_id AND l.is_deleted = 0
            ) AS likeCount,
            (
                SELECT COUNT(*)
                FROM feed_comment c
                WHERE c.post_id = p.post_id AND c.is_deleted = 0
            ) AS commentCount,
            EXISTS(
                SELECT 1
                FROM feed_like ml
                WHERE ml.post_id = p.post_id
                    AND ml.user_id = :currentUserId
                    AND ml.is_deleted = 0
            ) AS isLikedByMe,
            p.created_at AS createdAt
        FROM feed_post p
        LEFT JOIN item i ON i.item_id = p.item_id
        LEFT JOIN category c ON c.category_id = i.category_id
        LEFT JOIN user_profile_cache u ON u.user_id = p.owner_id
        WHERE p.is_deleted = 0
        AND p.sync_status = 'SYNCED'
        AND NOT EXISTS(
            SELECT 1
            FROM feed_hidden_post hp
            WHERE hp.post_id = p.post_id
                AND hp.user_id = :currentUserId
        )
        AND (
            p.owner_id = :currentUserId
            OR p.visibility = 'public'
            OR (
                p.visibility = 'friends'
                AND EXISTS(
                    SELECT 1
                    FROM friendship f
                    WHERE f.status = 'accepted'
                        AND f.is_deleted = 0
                        AND (
                            (f.sender_id = :currentUserId AND f.receiver_id = p.owner_id)
                            OR (f.receiver_id = :currentUserId AND f.sender_id = p.owner_id)
                        )
                )
            )
        )
        ORDER BY p.created_at DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun observePosts(
        currentUserId: String,
        limit: Int,
        offset: Int
    ): Flow<List<FeedPostDto>>

    @Query(
        """
        SELECT
            p.post_id AS postId,
            p.owner_id AS ownerId,
            COALESCE(u.display_name, p.owner_name) AS ownerName,
            u.avatar_url AS ownerAvatarUrl,
            p.item_id AS itemId,
            CASE
                WHEN p.item_id IS NULL AND instr(p.caption, char(10)) > 0
                    THEN trim(substr(p.caption, 1, instr(p.caption, char(10)) - 1))
                WHEN p.item_id IS NULL
                    THEN p.caption
                ELSE i.name
            END AS itemName,
            c.icon_url AS categoryIconUrl,
            p.image_url AS imageUrl,
            CASE
                WHEN p.item_id IS NULL AND instr(p.caption, char(10)) > 0
                    THEN trim(substr(p.caption, instr(p.caption, char(10)) + 1))
                WHEN p.item_id IS NULL
                    THEN ''
                ELSE p.caption
            END AS caption,
            p.visibility AS visibility,
            (
                SELECT COUNT(*)
                FROM feed_like l
                WHERE l.post_id = p.post_id AND l.is_deleted = 0
            ) AS likeCount,
            (
                SELECT COUNT(*)
                FROM feed_comment c
                WHERE c.post_id = p.post_id AND c.is_deleted = 0
            ) AS commentCount,
            EXISTS(
                SELECT 1
                FROM feed_like ml
                WHERE ml.post_id = p.post_id
                    AND ml.user_id = :currentUserId
                    AND ml.is_deleted = 0
            ) AS isLikedByMe,
            p.created_at AS createdAt
        FROM feed_post p
        LEFT JOIN item i ON i.item_id = p.item_id
        LEFT JOIN category c ON c.category_id = i.category_id
        LEFT JOIN user_profile_cache u ON u.user_id = p.owner_id
        WHERE p.is_deleted = 0
        AND p.sync_status = 'SYNCED'
        AND NOT EXISTS(
            SELECT 1
            FROM feed_hidden_post hp
            WHERE hp.post_id = p.post_id
                AND hp.user_id = :currentUserId
        )
        AND p.owner_id = :ownerId
        AND (
            p.owner_id = :currentUserId
            OR p.visibility = 'public'
            OR (
                p.visibility = 'friends'
                AND EXISTS(
                    SELECT 1
                    FROM friendship f
                    WHERE f.status = 'accepted'
                        AND f.is_deleted = 0
                        AND (
                            (f.sender_id = :currentUserId AND f.receiver_id = p.owner_id)
                            OR (f.receiver_id = :currentUserId AND f.sender_id = p.owner_id)
                        )
                )
            )
        )
        ORDER BY p.created_at DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun observePostsByOwner(
        ownerId: String,
        currentUserId: String,
        limit: Int,
        offset: Int
    ): Flow<List<FeedPostDto>>

    @Query(
        """
        SELECT
            c.comment_id AS commentId,
            c.post_id AS postId,
            c.user_id AS userId,
            COALESCE(u.display_name, c.display_name) AS displayName,
            u.avatar_url AS avatarUrl,
            c.body AS body,
            c.parent_comment_id AS parentCommentId,
            c.is_hidden AS isHidden,
            c.created_at AS createdAt,
            c.updated_at AS updatedAt
        FROM feed_comment c
        LEFT JOIN user_profile_cache u ON u.user_id = c.user_id
        WHERE c.post_id = :postId
            AND c.is_deleted = 0
            AND (
                c.is_hidden = 0
                OR EXISTS(
                    SELECT 1
                    FROM feed_post p
                    WHERE p.post_id = c.post_id
                        AND p.owner_id = :currentUserId
                )
            )
        ORDER BY c.created_at ASC
        """
    )
    fun observeComments(postId: String, currentUserId: String): Flow<List<FeedCommentDto>>

    @Query(
        """
        SELECT
            i.item_id AS itemId,
            i.name AS name,
            i.image_url AS imageUrl,
            c.icon_url AS categoryIconUrl,
            i.price AS price,
            i.entry_date AS entryDate
        FROM item i
        LEFT JOIN category c ON c.category_id = i.category_id
        WHERE i.is_deleted = 0
        ORDER BY i.created_at DESC
        LIMIT :limit
        """
    )
    fun observeSourceItems(limit: Int = 30): Flow<List<FeedSourceItemDto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: FeedPost)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<FeedPost>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfiles(profiles: List<UserProfileCacheEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: FeedComment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenPost(hiddenPost: FeedHiddenPost)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<FeedComment>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLikes(likes: List<FeedLike>)

    @Upsert
    suspend fun upsertLike(like: FeedLike)

    @Query("SELECT * FROM feed_post WHERE sync_status = 'PENDING' OR sync_status = 'FAILED'")
    suspend fun getPendingPosts(): List<FeedPost>

    @Query(
        """
        SELECT post_id
        FROM feed_post
        WHERE is_deleted = 1 AND (sync_status = 'PENDING' OR sync_status = 'FAILED')
        """
    )
    suspend fun getPendingDeletedPostIds(): List<String>

    @Query(
        """
        SELECT post_id
        FROM feed_post
        WHERE is_deleted = 1
        """
    )
    suspend fun getDeletedPostIds(): List<String>

    @Query("SELECT * FROM feed_like WHERE sync_status = 'PENDING' OR sync_status = 'FAILED'")
    suspend fun getPendingLikes(): List<FeedLike>

    @Query("SELECT * FROM feed_comment WHERE sync_status = 'PENDING' OR sync_status = 'FAILED'")
    suspend fun getPendingComments(): List<FeedComment>

    @Query("SELECT * FROM feed_like WHERE post_id = :postId AND user_id = :userId LIMIT 1")
    suspend fun getLike(postId: String, userId: String): FeedLike?

    @Query("SELECT * FROM feed_post WHERE post_id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): FeedPost?

    @Query("SELECT * FROM feed_comment WHERE comment_id = :commentId LIMIT 1")
    suspend fun getCommentById(commentId: String): FeedComment?

    @Query("UPDATE feed_post SET image_url = :imageUrl, updated_at = :updatedAt WHERE post_id = :postId")
    suspend fun updatePostImageUrl(postId: String, imageUrl: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM feed_post WHERE sync_status = 'SYNCED'")
    suspend fun deleteAllSyncedPosts()

    @Query("DELETE FROM feed_post WHERE sync_status = 'SYNCED' AND post_id NOT IN (:remotePostIds)")
    suspend fun deleteSyncedPostsMissingFromRemote(remotePostIds: List<String>)

    @Query(
        """
        UPDATE feed_post
        SET is_deleted = 1, sync_status = 'SYNCED', updated_at = :updatedAt
        WHERE sync_status = 'SYNCED'
        AND is_deleted = 0
        """
    )
    suspend fun softDeleteAllSyncedPosts(updatedAt: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE feed_post
        SET is_deleted = 1, sync_status = 'SYNCED', updated_at = :updatedAt
        WHERE sync_status = 'SYNCED'
        AND is_deleted = 0
        AND post_id NOT IN (:remotePostIds)
        """
    )
    suspend fun softDeleteSyncedPostsMissingFromRemote(
        remotePostIds: List<String>,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE feed_post
        SET is_deleted = 1, sync_status = 'SYNCED', updated_at = :updatedAt
        WHERE sync_status = 'SYNCED'
        AND post_id IN (:remotePostIds)
        """
    )
    suspend fun softDeleteSyncedPostsByRemoteIds(
        remotePostIds: List<String>,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE feed_post SET sync_status = 'SYNCED' WHERE post_id = :postId")
    suspend fun markPostSynced(postId: String)

    @Query("UPDATE feed_post SET sync_status = 'FAILED' WHERE post_id = :postId")
    suspend fun markPostFailed(postId: String)

    @Query("UPDATE feed_like SET sync_status = 'SYNCED' WHERE like_id = :likeId")
    suspend fun markLikeSynced(likeId: String)

    @Query("UPDATE feed_like SET sync_status = 'FAILED' WHERE like_id = :likeId")
    suspend fun markLikeFailed(likeId: String)

    @Query(
        """
        UPDATE feed_like
        SET is_deleted = 1, sync_status = 'SYNCED', updated_at = :updatedAt
        WHERE sync_status = 'SYNCED'
        AND is_deleted = 0
        """
    )
    suspend fun softDeleteAllSyncedLikes(updatedAt: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE feed_like
        SET is_deleted = 1, sync_status = 'SYNCED', updated_at = :updatedAt
        WHERE sync_status = 'SYNCED'
        AND is_deleted = 0
        AND like_id NOT IN (:remoteLikeIds)
        """
    )
    suspend fun softDeleteSyncedLikesMissingFromRemote(
        remoteLikeIds: List<String>,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE feed_like
        SET is_deleted = 1, sync_status = 'SYNCED', updated_at = :updatedAt
        WHERE post_id = :postId
        AND user_id = :userId
        AND sync_status = 'SYNCED'
        """
    )
    suspend fun softDeleteSyncedLikeByRemoteKey(
        postId: String,
        userId: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE feed_comment SET sync_status = 'SYNCED' WHERE comment_id = :commentId")
    suspend fun markCommentSynced(commentId: String)

    @Query("UPDATE feed_comment SET sync_status = 'FAILED' WHERE comment_id = :commentId")
    suspend fun markCommentFailed(commentId: String)

    @Query(
        """
        UPDATE feed_comment
        SET is_deleted = 1, sync_status = 'SYNCED', updated_at = :updatedAt
        WHERE sync_status = 'SYNCED'
        """
    )
    suspend fun softDeleteAllSyncedComments(updatedAt: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE feed_comment
        SET is_deleted = 1, sync_status = 'SYNCED', updated_at = :updatedAt
        WHERE sync_status = 'SYNCED'
        AND comment_id NOT IN (:remoteCommentIds)
        """
    )
    suspend fun softDeleteSyncedCommentsMissingFromRemote(
        remoteCommentIds: List<String>,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE feed_comment
        SET body = :body, sync_status = 'PENDING', updated_at = :updatedAt
        WHERE comment_id = :commentId AND user_id = :userId AND is_deleted = 0
        """
    )
    suspend fun updateCommentBody(
        commentId: String,
        userId: String,
        body: String,
        updatedAt: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        WITH RECURSIVE comment_tree(comment_id) AS (
            SELECT comment_id
            FROM feed_comment
            WHERE comment_id = :commentId
                AND user_id = :currentUserId
                AND is_deleted = 0

            UNION ALL

            SELECT child.comment_id
            FROM feed_comment child
            INNER JOIN comment_tree parent ON child.parent_comment_id = parent.comment_id
            WHERE child.is_deleted = 0
        )
        UPDATE feed_comment
        SET is_deleted = 1,
            sync_status = CASE
                WHEN comment_id = :commentId THEN 'PENDING'
                ELSE 'SYNCED'
            END,
            updated_at = :updatedAt
        WHERE comment_id IN (SELECT comment_id FROM comment_tree)
        """
    )
    suspend fun softDeleteCommentThread(
        commentId: String,
        currentUserId: String,
        updatedAt: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE feed_comment
        SET is_hidden = :isHidden,
            hidden_at = CASE WHEN :isHidden THEN :updatedAt ELSE NULL END,
            sync_status = 'SYNCED',
            updated_at = :updatedAt
        WHERE comment_id = :commentId
        AND is_deleted = 0
        AND EXISTS(
            SELECT 1
            FROM feed_post p
            WHERE p.post_id = feed_comment.post_id
                AND p.owner_id = :currentUserId
        )
        """
    )
    suspend fun setCommentHidden(
        commentId: String,
        currentUserId: String,
        isHidden: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE feed_like
        SET is_deleted = :isDeleted, sync_status = 'PENDING', updated_at = :updatedAt
        WHERE like_id = :likeId
        """
    )
    suspend fun setLikeDeleted(likeId: String, isDeleted: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE feed_post
        SET is_deleted = 1, sync_status = 'PENDING', updated_at = :updatedAt
        WHERE post_id = :postId AND owner_id = :ownerId
        """
    )
    suspend fun softDeletePost(
        postId: String,
        ownerId: String,
        updatedAt: Long = System.currentTimeMillis()
    ): Int

    @Transaction
    suspend fun toggleLike(postId: String, userId: String) {
        val now = System.currentTimeMillis()
        val existing = getLike(postId, userId)
        if (existing == null) {
            upsertLike(
                FeedLike(
                    postId = postId,
                    userId = userId,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            setLikeDeleted(existing.likeId, !existing.isDeleted, now)
        }
    }
}
