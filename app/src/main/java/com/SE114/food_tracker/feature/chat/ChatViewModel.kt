package com.SE114.food_tracker.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.data.local.dao.ChatDAO
import com.SE114.food_tracker.data.local.entities.Conversation
import com.SE114.food_tracker.data.local.entities.Message
import com.SE114.food_tracker.data.local.entities.MessageSyncStatus
import com.SE114.food_tracker.data.repository.ChatRepository
import com.SE114.food_tracker.feature.chat.components.MessageUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatDAO: ChatDAO // thực hiện mồi dữ liệu local
) : ViewModel() {

    val currentUserId = "vy_id"

    // KHỐI KHỞI TẠO ĐÃ ĐƯỢC TỐI ƯU TRÁNH GHI ĐÈ DUPLICATE DATA KHI RE-OPEN VIEWMODEL
    init {
        viewModelScope.launch {
            // 1. Tạo sẵn một cuộc trò chuyện mẫu với ID cố định là "1" nếu chưa tồn tại
            chatDAO.insertConversation(
                Conversation(
                    id = "1",
                    isGroup = false,
                    name = "Azun (Data)",
                    walletId = "mock_wallet_id_1"
                )
            )

            // 2. CHỈ MỒI TIN NHẮN KHI PHÒNG CHAT ĐANG TRỐNG TRƠN
            // Lấy danh sách tin nhắn hiện tại của cuộc trò chuyện "1" để kiểm tra
            val currentMessages = chatRepository.getMessagesStream("1").first()

            if (currentMessages.isEmpty()) {
                // Cố định mốc thời gian lịch sử cụ thể để tin nhắn cũ không bị nhảy giờ lung tung
                val oneMinuteAgo = System.currentTimeMillis() - 60000
                val thirtySecondsAgo = System.currentTimeMillis() - 30000

                chatDAO.insertMessage(
                    Message(
                        localId = "mock_msg_init_1",
                        serverId = "srv_init_1",
                        conversationId = "1",
                        senderId = "azun_id",
                        body = "Ăn bún bò Huế đi zz",
                        imageUrl = null,
                        isSystem = false,
                        syncStatus = MessageSyncStatus.SENT,
                        createdAt = oneMinuteAgo
                    )
                )

                chatDAO.insertMessage(
                    Message(
                        localId = "mock_msg_init_2",
                        serverId = "srv_init_2",
                        conversationId = "1",
                        senderId = "system",
                        body = "Hệ thống: Vy đã nộp 100k vào quỹ nhóm",
                        imageUrl = null,
                        isSystem = true,
                        syncStatus = MessageSyncStatus.SENT,
                        createdAt = thirtySecondsAgo
                    )
                )
            }
        }
    }

    // 1. Lấy tin nhắn từ Room và tự động định dạng Ngày/Giờ thật sang dữ liệu UI
    fun getMessagesState(conversationId: String): Flow<List<MessageUiModel>> {
        return chatRepository.getMessagesStream(conversationId).map { entities ->
            entities.map { entity ->
                MessageUiModel(
                    localId = entity.localId,
                    senderId = entity.senderId,
                    body = entity.body,
                    imageUrl = entity.imageUrl,
                    isSystem = entity.isSystem,
                    syncStatus = entity.syncStatus,
                    // Đổi số Long thành Giờ thật
                    timeLabel = formatToTime(entity.createdAt),
                    dateLabel = formatToDate(entity.createdAt),
                    rawEntity = entity // Lưu lại thực thể gốc để phục vụ hàm Retry
                )
            }
        }
    }

    // 2. Phát lệnh gửi tin nhắn Text
    fun sendTextMessage(conversationId: String, text: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(conversationId, currentUserId, body = text, imageUrl = null)
        }
    }

    // 3. Phát lệnh gửi tin nhắn Ảnh (Giai đoạn này xử lý truyền URI thật từ thiết bị)
    fun sendImageMessage(conversationId: String, imageUri: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(
                conversationId, currentUserId, body = null, imageUrl = imageUri
            )
        }
    }

    // 4. Phát lệnh thử lại tin nhắn bị lỗi
    fun retryFailedMessage(messageEntity: Message) {
        viewModelScope.launch {
            chatRepository.retryMessage(messageEntity)
        }
    }

    // 5. Lấy dòng chảy danh sách hội thoại đổ lên màn hình List
    fun getConversationsFlow(): Flow<List<Conversation>> {
        return chatDAO.getAllConversations()
    }

    // --- CÁC HÀM TIỆN ÍCH CHUYỂN ĐỔI NGÀY GIỜ THẬT ---
    private fun formatToTime(timestamp: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatToDate(timestamp: Long): String {
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()
        return if (target.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        ) {
            "Hôm nay"
        } else {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}