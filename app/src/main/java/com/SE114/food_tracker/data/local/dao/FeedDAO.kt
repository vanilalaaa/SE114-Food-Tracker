package com.SE114.food_tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.SE114.food_tracker.data.local.entities.FeedComment
import com.SE114.food_tracker.data.local.entities.FeedLike
import com.SE114.food_tracker.data.local.entities.FeedPost
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDAO {

    @Query(
        """
        SELECT
            p.post_id AS postId,
            p.owner_id AS ownerId,
            p.owner_name AS ownerName,
            p.item_id AS itemId,
            i.name AS itemName,
            p.image_url AS imageUrl,
            p.caption AS caption,
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
        WHERE p.is_deleted = 0
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
            comment_id AS commentId,
            post_id AS postId,
            user_id AS userId,
            display_name AS displayName,
            body,
            created_at AS createdAt
        FROM feed_comment
        WHERE post_id = :postId AND is_deleted = 0
        ORDER BY created_at ASC
        """
    )
    fun observeComments(postId: String): Flow<List<FeedCommentDto>>

    @Query(
        """
        SELECT item_id AS itemId, name, image_url AS imageUrl, price, entry_date AS entryDate
        FROM item
        WHERE is_deleted = 0
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    fun observeSourceItems(limit: Int = 30): Flow<List<FeedSourceItemDto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: FeedPost)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: FeedComment)

    @Upsert
    suspend fun upsertLike(like: FeedLike)

    @Query("SELECT * FROM feed_like WHERE post_id = :postId AND user_id = :userId LIMIT 1")
    suspend fun getLike(postId: String, userId: String): FeedLike?

    @Query(
        """
        UPDATE feed_like
        SET is_deleted = :isDeleted, sync_status = 'PENDING', updated_at = :updatedAt
        WHERE like_id = :likeId
        """
    )
    suspend fun setLikeDeleted(likeId: String, isDeleted: Boolean, updatedAt: Long)

    @Query("UPDATE feed_post SET is_deleted = 1, sync_status = 'PENDING', updated_at = :updatedAt WHERE post_id = :postId")
    suspend fun softDeletePost(postId: String, updatedAt: Long = System.currentTimeMillis())

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
