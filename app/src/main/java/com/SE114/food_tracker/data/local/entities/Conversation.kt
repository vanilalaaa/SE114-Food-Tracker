package com.SE114.food_tracker.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey
    val id: String, // Chuỗi UUID từ Supabase

    @ColumnInfo(name = "is_group")
    val isGroup: Boolean = false,

    val name: String?,

    @ColumnInfo(name = "wallet_id")
    val walletId: String?, // ID liên kết ví nhóm

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)