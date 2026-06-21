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

    // ── STATE QUẢN LÝ DỮ LIỆU QUỸ NHÓM ──
    var walletBalance by mutableStateOf(0.0)
        private set

    private val _walletTransactions =
        MutableStateFlow<List<Map<String, kotlinx.serialization.json.JsonElement>>>(emptyList())
    val walletTransactions: StateFlow<List<Map<String, kotlinx.serialization.json.JsonElement>>> =
        _walletTransactions.asStateFlow()

    var isTransactionSuccess by mutableStateOf<Boolean?>(null)
        private set

    init {
        fetchConversationsFromServer()
    }

    fun fetchConversationsFromServer() {
        viewModelScope.launch {
            try {
                chatRepository.fetchAndSaveConversationsToLocal()
            } catch (e: Exception) {
                println("Lỗi kéo danh sách hội thoại từ server: ${e.localizedMessage}")
            }
        }
    }

    // 🔥 ĐÃ FIX: Biến StateFlow quản lý tập trung danh sách thành viên nhóm
    private val _groupMembers = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val groupMembers: StateFlow<List<Pair<String, String>>> = _groupMembers.asStateFlow()

    // Hàm gọi bốc dữ liệu một lần duy nhất khi vào phòng chat
    fun loadGroupMembers(conversationId: String) {
        viewModelScope.launch {
            chatRepository.syncMessagesFromServer(conversationId)
            val actualMembers = chatRepository.fetchGroupMembersFromServer(conversationId)
            _groupMembers.value = actualMembers
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

    // Lấy luồng thông tin một phòng chat cụ thể từ Room DB để lấy Tên và ID ví
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

    // Hàm xử lý kích hoạt giao dịch từ Giao diện gửi lên thẳng RPC server thật
    fun executeWalletTransaction(
        conversationId: String,
        amount: Double,
        isDeposit: Boolean,
        note: String
    ) {
        viewModelScope.launch {
            val transactionType = if (isDeposit) "deposit" else "withdrawal"

            val success = chatRepository.executeWalletTransaction(
                conversationId = conversationId,
                amount = amount,
                txType = transactionType,
                note = note,
                itemId = null
            )

            if (success) {
                loadWalletData(conversationId)
            }
            isTransactionSuccess = success
        }
    }

    fun resetTransactionStatus() {
        isTransactionSuccess = null
    }

    // ── PHÂN QUYỀN ADMIN & KHỞI TẠO QUỸ ──

    // Kiểm tra bất đồng bộ quyền Admin thật của người dùng trong phòng chat dựa trên Repository
    suspend fun isCurrentUserAdmin(conversationId: String): Boolean {
        return try {
            chatRepository.isCurrentUserAdminOf(conversationId)
        } catch (e: Exception) {
            println("Lỗi kiểm tra quyền Admin thật: ${e.localizedMessage}")
            false
        }
    }

    // Khởi tạo Quỹ thật từ giao diện và đồng bộ lại danh sách hội thoại cục bộ
    fun createGroupWallet(conversationId: String, walletName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = chatRepository.createGroupWalletForExistingChat(
                    conversationId = conversationId,
                    walletName = walletName,
                    memberUserIds = emptyList()
                )
                if (success) {
                    fetchConversationsFromServer() // Refresh dữ liệu local Room DB để cập nhật wallet_id mới
                }
                onResult(success)
            } catch (e: Exception) {
                println("Lỗi luồng xử lý đúc ví trên server: ${e.localizedMessage}")
                onResult(false)
            }
        }
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