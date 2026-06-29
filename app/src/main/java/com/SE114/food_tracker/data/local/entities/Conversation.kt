package com.SE114.food_tracker.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "is_group")
    val isGroup: Boolean = false,

    val name: String?,

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String? = null,

    @ColumnInfo(name = "last_message_at")
    val lastMessageAt: Long = 0L,

    @ColumnInfo(name = "last_message_snippet")
    val lastMessageSnippet: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)