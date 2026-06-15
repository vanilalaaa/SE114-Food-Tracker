package com.SE114.food_tracker.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "friendship",
    indices = [Index(value = ["sender_id", "receiver_id"], unique = true)]
)
data class FriendshipEntity(
    @PrimaryKey
    @ColumnInfo(name = "friendship_id")
    val friendshipId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "sender_id")
    val senderId: String,

    @ColumnInfo(name = "receiver_id")
    val receiverId: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "PENDING",

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)