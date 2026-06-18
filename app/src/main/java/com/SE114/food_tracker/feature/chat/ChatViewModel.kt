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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatDAO: ChatDAO
) : ViewModel() {

    // LẤY USER ID THẬT TỪ AUTH ĐỂ KHỚP VỚI POLICY MÀ KHÔNG DÙNG ID GIẢ LẬP
    val currentUserId: String
        get() = chatRepository.getAuthenticatedUserId()

    // 🌟 SẠCH BÓNG MOCK DATA: Khối khởi tạo trống trơn để nhường chỗ cho dữ liệu liên thông thật
    init {
        // Không mồi dữ liệu giả lập nữa
    }

    // 🌟 KÍCH HOẠT LẮNG NGHE REALTIME ĐỘNG: Được gọi trực tiếp khi bấm vào bất kỳ phòng nào ở list bên ngoài
    fun connectToConversation(conversationId: String) {
        viewModelScope.launch {
            chatRepository.subscribeToChatRealtime(conversationId)
        }
    }

    // Tương tác Chat nhóm từ giao diện UI điều phối xuống Repository
    fun createGroup(name: String, members: List<String>) {
        viewModelScope.launch { chatRepository.createGroupChat(name, members) }
    }

    fun renameGroup(conversationId: String, newName: String) {
        viewModelScope.launch { chatRepository.updateGroupName(conversationId, newName) }
    }

    fun kickGroupMember(conversationId: String, userId: String, name: String) {
        viewModelScope.launch { chatRepository.kickMember(conversationId, userId, name) }
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
                    timeLabel = formatToTime(entity.createdAt),
                    dateLabel = formatToDate(entity.createdAt),
                    rawEntity = entity
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

    // 3. Phát lệnh gửi tin nhắn Ảnh
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