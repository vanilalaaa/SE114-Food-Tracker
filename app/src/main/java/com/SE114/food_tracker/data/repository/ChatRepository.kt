package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.data.local.dao.ChatDAO
import com.SE114.food_tracker.data.local.entities.Message
import com.SE114.food_tracker.data.local.entities.MessageSyncStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDAO: ChatDAO
) {
    // 1. Lấy dòng chảy tin nhắn realtime từ Room DB lên UI
    fun getMessagesStream(conversationId: String): Flow<List<Message>> {
        return chatDAO.getMessagesByConversation(conversationId)
    }

    // 2. Logic Gửi Tin Nhắn & Xử lý Hàng đợi Offline (Queue)
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        body: String?,
        imageUrl: String?
    ) {
        val localId = UUID.randomUUID().toString()

        // Tạo thực thể tin nhắn tạm thời với trạng thái PENDING
        val pendingMessage = Message(
            localId = localId,
            serverId = null, // Chưa lên server
            conversationId = conversationId,
            senderId = senderId,
            body = body,
            imageUrl = imageUrl,
            isSystem = false,
            syncStatus = MessageSyncStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        // Ghi xuống Room DB ngay lập tức để UI cập nhật icon Đồng hồ xoay 🕒
        chatDAO.insertMessage(pendingMessage)

        // Kích hoạt tiến trình gửi dữ liệu lên Network (Giả lập Queue)
        performNetworkSend(pendingMessage)
    }

    // 3. Logic Thử lại (Retry) khi tin nhắn bị lỗi 🚨
    suspend fun retryMessage(message: Message) {
        // Chuyển trạng thái quay lại PENDING để hiện icon đồng hồ
        val reloadingMessage = message.copy(syncStatus = MessageSyncStatus.PENDING)
        chatDAO.updateMessage(reloadingMessage)

        // Đẩy lại lên Network
        performNetworkSend(reloadingMessage)
    }

    // 4. Hàm giả lập đẩy dữ liệu lên Server (Sẽ thế chỗ này bằng API Supabase)
    private suspend fun performNetworkSend(message: Message) {
        try {
            // Giả lập độ trễ mạng 1.5 giây để test hiệu ứng xoay đồng hồ trên UI
            delay(1500)

            // Giả lập tình huống ngẫu nhiên để test cờ lỗi FAILED (Xác suất 20% lỗi mạng)
            val isNetworkAvailable = (1..5).random() != 1

            if (isNetworkAvailable) {
                // THÀNH CÔNG: Cập nhật cờ SENT (Hiện dấu tick xanh ✓) và gán ID giả lập từ Server
                val successMessage = message.copy(
                    syncStatus = MessageSyncStatus.SENT,
                    serverId = UUID.randomUUID().toString()
                )
                chatDAO.updateMessage(successMessage)
            } else {
                // THẤT BẠI: Mất mạng hoặc Server sập -> Chuyển sang FAILED (Hiện dấu chấm đỏ 🚨)
                val failedMessage = message.copy(syncStatus = MessageSyncStatus.FAILED)
                chatDAO.updateMessage(failedMessage)
            }
        } catch (e: Exception) {
            val failedMessage = message.copy(syncStatus = MessageSyncStatus.FAILED)
            chatDAO.updateMessage(failedMessage)
        }
    }
}