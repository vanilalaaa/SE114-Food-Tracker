package com.SE114.food_tracker.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "conversation_participants",
    primaryKeys = ["conversation_id", "user_id"]
)
data class ConversationParticipant(
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "is_admin")
    val isAdmin: Boolean = false, // Cờ kiểm tra Admin nhóm

    @ColumnInfo(name = "joined_at")
    val joinedAt: Long = System.currentTimeMillis()
)