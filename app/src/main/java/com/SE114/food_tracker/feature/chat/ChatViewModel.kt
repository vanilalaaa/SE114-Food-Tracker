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
import kotlinx.coroutines.flow.first

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
        chatRepository.subscribeToGlobalConversationsRealtime()
        viewModelScope.launch {
            chatRepository.memberUpdateSignal.collect { convId: String ->
                // Tự động load lại danh sách thành viên vào StateFlow
                loadGroupMembers(convId)
            }
            chatRepository.walletUpdateSignal.collect { convId ->
                // Nạp lại data ví tĩnh thành data động
                loadWalletData(convId)
            }
        }
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

    private val _groupMembers = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val groupMembers: StateFlow<List<Pair<String, String>>> = _groupMembers.asStateFlow()
    private val _isCurrentAdmin = MutableStateFlow(false)
    val isCurrentAdmin: StateFlow<Boolean> = _isCurrentAdmin.asStateFlow()

    fun loadGroupMembers(conversationId: String) {
        viewModelScope.launch {
            val isAdmin = chatRepository.isCurrentUserAdminOf(conversationId)
            _isCurrentAdmin.value = isAdmin
            val actualMembers = chatRepository.fetchGroupMembersFromServer(conversationId)
            _groupMembers.value = actualMembers
        }
    }

    fun refreshChatData(conversationId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                chatRepository.syncMessagesFromServer(conversationId)
                val actualMembers = chatRepository.fetchGroupMembersFromServer(conversationId)
                _groupMembers.value = actualMembers
                val isAdmin = chatRepository.isCurrentUserAdminOf(conversationId)
                _isCurrentAdmin.value = isAdmin
            } catch (e: Exception) {
                println("Lỗi khi kéo reload phòng chat: ${e.localizedMessage}")
            } finally {
                onComplete()
            }
        }
    }

    fun connectToConversation(conversationId: String) {
        viewModelScope.launch {
            chatRepository.subscribeToChatRealtime(conversationId)
        }
    }

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

    fun getConversationState(conversationId: String): Flow<Conversation?> {
        return chatRepository.getLocalConversation(conversationId)
    }


    fun getMessagesState(conversationId: String): Flow<List<MessageUiModel>> {

        connectToConversation(conversationId)
        viewModelScope.launch {
            try {
                // Kéo tin nhắn cũ về trước để Room có dữ liệu gốc
                chatRepository.syncMessagesFromServer(conversationId)
                // Kéo tiếp profile thành viên về để map đè lên
                chatRepository.fetchGroupMembersFromServer(conversationId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return chatRepository.getMessagesWithProfileStream(conversationId).map { joinEntities ->
            joinEntities.map { entity ->
                val finalName = when {
                    entity.isSystem || entity.senderId == "system" -> "Hệ thống"
                    !entity.cacheName.isNullOrBlank() -> entity.cacheName
                    else -> "Thành viên"
                }

                MessageUiModel(
                    localId = entity.id ?: java.util.UUID.randomUUID().toString(),
                    senderId = entity.senderId ?: "",
                    body = entity.body,
                    imageUrl = entity.imageUrl,
                    isSystem = entity.isSystem,
                    syncStatus = entity.syncStatus,
                    timeLabel = formatToTime(entity.createdAt),
                    dateLabel = formatToDate(entity.createdAt),
                    senderName = finalName,
                    senderAvatarUrl = entity.cacheAvatar ?: ""
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

    private val _walletBalanceFlow = MutableStateFlow(0.0)
    val walletBalanceFlow: StateFlow<Double> = _walletBalanceFlow.asStateFlow()

    fun loadWalletData(conversationId: String) {
        viewModelScope.launch {
            val balance = chatRepository.getWalletBalance(conversationId)
            _walletBalanceFlow.value = balance
            walletBalance = balance
            _walletTransactions.value =
                chatRepository.fetchWalletTransactionsFromServer(conversationId)
        }
    }

    fun executeWalletTransaction(
        conversationId: String,
        amount: Double,
        isDeposit: Boolean,
        note: String
    ) {
        viewModelScope.launch {
            try {
                val transactionType = if (isDeposit) "deposit" else "withdrawal"
                val conversation = chatDAO.getConversationById(conversationId).first()
                val actualWalletId = conversation?.walletId

                if (actualWalletId.isNullOrBlank() || actualWalletId == "wallet_default") return@launch

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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetTransactionStatus() {
        isTransactionSuccess = null
    }

    suspend fun isCurrentUserAdmin(conversationId: String): Boolean {
        return try {
            chatRepository.isCurrentUserAdminOf(conversationId)
        } catch (e: Exception) {
            false
        }
    }

    fun createGroupWallet(conversationId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = chatRepository.createGroupWalletForExistingChat(
                    conversationId = conversationId,
                    memberUserIds = emptyList()
                )
                if (success) {
                    fetchConversationsFromServer()
                }
                onResult(success)
            } catch (e: Exception) {
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