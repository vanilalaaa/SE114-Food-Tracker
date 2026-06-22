package com.SE114.food_tracker.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "feed_like",
    indices = [
        Index(value = ["post_id", "user_id"], unique = true),
        Index("sync_status")
    ]
)
data class FeedLike(
    @PrimaryKey
    @ColumnInfo(name = "like_id")
    val likeId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "post_id")
    val postId: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "PENDING",

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
