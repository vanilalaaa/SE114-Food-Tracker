package com.SE114.food_tracker.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MessageSyncStatus {
    PENDING, // Đồng hồ xoay
    SENT,    // Dấu tích xanh
    FAILED   // Chấm đỏ cảnh báo
}

// Index on (conversation_id, created_at): the conversation list's per-conversation unread COUNT
// and the message stream both filter by conversation_id and order/compare by created_at, so
// without this index each runs a full table scan — the source of the "laggy on open" feel.
@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversation_id", "created_at"])]
)
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