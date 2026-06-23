package com.SE114.food_tracker.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "feed_comment",
    indices = [
        Index("post_id"),
        Index("parent_comment_id"),
        Index("user_id"),
        Index("sync_status")
    ]
)
data class FeedComment(
    @PrimaryKey
    @ColumnInfo(name = "comment_id")
    val commentId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "post_id")
    val postId: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    val body: String,

    @ColumnInfo(name = "parent_comment_id")
    val parentCommentId: String? = null,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "PENDING",

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
