package com.SE114.food_tracker.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "feed_post",
    indices = [
        Index("owner_id"),
        Index("item_id"),
        Index("created_at"),
        Index("sync_status")
    ]
)
data class FeedPost(
    @PrimaryKey
    @ColumnInfo(name = "post_id")
    val postId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "owner_id")
    val ownerId: String,

    @ColumnInfo(name = "owner_name")
    val ownerName: String,

    @ColumnInfo(name = "item_id")
    val itemId: String? = null,

    @ColumnInfo(name = "image_url")
    val imageUrl: String,

    val caption: String,

    val visibility: String = "friends",

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "PENDING",

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
