package com.SE114.food_tracker.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageSyncStatus {
    PENDING, // Đồng hồ xoay
    SENT,    // Dấu tích xanh
    FAILED   // Chấm đỏ cảnh báo
}

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    @ColumnInfo(name = "local_id")
    val localId: String, // UUID tạo ngay tại máy local khi bấm gửi tin

    @ColumnInfo(name = "id")
    val serverId: String?, // ID server (UUID) nhận từ Supabase, nhận null khi pending

    @ColumnInfo(name = "conversation_id")
    val conversationId: String,

    @ColumnInfo(name = "sender_id")
    val senderId: String,

    val body: String?, // Tách riêng nội dung chữ

    @ColumnInfo(name = "image_url")
    val imageUrl: String?, // Tách riêng đường dẫn tệp ảnh

    @ColumnInfo(name = "is_system")
    val isSystem: Boolean = false, // Phục vụ render tin nhắn hệ thống

    @ColumnInfo(name = "sync_status")
    val syncStatus: MessageSyncStatus = MessageSyncStatus.PENDING,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)