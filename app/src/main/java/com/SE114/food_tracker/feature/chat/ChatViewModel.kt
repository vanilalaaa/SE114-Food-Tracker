package com.SE114.food_tracker.feature.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val currentUserId: String
        get() = chatRepository.getAuthenticatedUserId()

    // ── STATE QUẢN LÝ DỮ LIỆU QUỸ NHÓM THẬT CHO UI QUAN SÁT ──
    var walletBalance by mutableStateOf(0.0)
        private set

    private val _walletTransactions =
        MutableStateFlow<List<Map<String, kotlinx.serialization.json.JsonElement>>>(emptyList())
    val walletTransactions: StateFlow<List<Map<String, kotlinx.serialization.json.JsonElement>>> =
        _walletTransactions.asStateFlow()

    var isTransactionSuccess by mutableStateOf<Boolean?>(null)
        private set

    init {
        // 1. Duy trì lệnh đồng bộ từ server như bình thường
        fetchConversationsFromServer()

        // 2. MỒI DATA VÀO ROOM LOCAL ĐỂ KIỂM TRA MÀN HÌNH LÊN ĐÈN
        viewModelScope.launch {
            try {
                val testRoom = Conversation(
                    id = "phong_test_114",
                    name = "Nhóm Quỹ Thực Tế 🥑",
                    isGroup = true,
                    walletId = "wallet_xyz123"
                )
                chatDAO.insertConversation(testRoom)
                println("ChatViewModel: Đã mồi thành công phòng chat test xuống Room local!")
            } catch (e: Exception) {
                println("Lỗi mồi dữ liệu local: ${e.localizedMessage}")
            }
        }
    }

    private fun fetchConversationsFromServer() {
        viewModelScope.launch {
            try {
                chatRepository.fetchAndSaveConversationsToLocal()
            } catch (e: Exception) {
                println("Lỗi kéo danh sách hội thoại từ server: ${e.localizedMessage}")
            }
        }
    }

    // KÍCH HOẠT LẮNG NGHE REALTIME ĐỘNG
    fun connectToConversation(conversationId: String) {
        viewModelScope.launch {
            chatRepository.subscribeToChatRealtime(conversationId)
        }
    }

    // Tự động làm mới danh sách cục bộ sau khi bấm nút + tạo nhóm thành công trên Server
    fun createGroup(name: String, members: List<String>) {
        viewModelScope.launch {
            chatRepository.createGroupChat(name, members)
            fetchConversationsFromServer()
        }
    }

    fun renameGroup(conversationId: String, newName: String) {
        viewModelScope.launch { chatRepository.updateGroupName(conversationId, newName) }
    }

    fun kickGroupMember(conversationId: String, userId: String, name: String) {
        viewModelScope.launch { chatRepository.kickMember(conversationId, userId, name) }
    }

    // Lấy luồng thông tin một phòng chat cụ thể từ Room DB để lấy Tên và ID ví sống
    fun getConversationState(conversationId: String): Flow<Conversation?> {
        return chatRepository.getLocalConversation(conversationId)
    }

    // Lấy tin nhắn từ Room và tự động định dạng Ngày/Giờ thật sang dữ liệu UI
    fun getMessagesState(conversationId: String): Flow<List<MessageUiModel>> {
        connectToConversation(conversationId)

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

    fun sendTextMessage(conversationId: String, text: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(conversationId, currentUserId, body = text, imageUrl = null)
        }
    }

    fun sendImageMessage(conversationId: String, imageUri: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(
                conversationId, currentUserId, body = null, imageUrl = imageUri
            )
        }
    }

    fun retryFailedMessage(messageEntity: Message) {
        viewModelScope.launch {
            chatRepository.retryMessage(messageEntity)
        }
    }

    fun getConversationsFlow(): Flow<List<Conversation>> {
        return chatDAO.getAllConversations()
    }


    // Hàm gọi bốc số dư và lịch sử giao dịch thật từ Server về máy
    fun loadWalletData(conversationId: String) {
        viewModelScope.launch {
            walletBalance = chatRepository.getWalletBalance(conversationId)
            _walletTransactions.value =
                chatRepository.fetchWalletTransactionsFromServer(conversationId)
        }
    }

    // Hàm kích hoạt thực hiện giao dịch nộp hoặc rút quỹ thật
    fun executeWalletTransaction(
        conversationId: String,
        amount: Double,
        isDeposit: Boolean,
        note: String
    ) {
        viewModelScope.launch {
            val success =
                chatRepository.executeWalletTransaction(conversationId, amount, isDeposit, note)
            if (success) {
                // Tự động load lại số dư và lịch sử giao dịch mới nhất nếu thành công
                loadWalletData(conversationId)
            }
            isTransactionSuccess = success
        }
    }

    fun resetTransactionStatus() {
        isTransactionSuccess = null
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