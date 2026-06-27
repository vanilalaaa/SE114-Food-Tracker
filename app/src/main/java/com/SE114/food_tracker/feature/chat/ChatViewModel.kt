package com.SE114.food_tracker.feature.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.data.local.dao.ChatDAO
import com.SE114.food_tracker.data.local.dao.ConversationWithUnread
import com.SE114.food_tracker.data.local.entities.Conversation
import com.SE114.food_tracker.data.local.entities.Message
import com.SE114.food_tracker.data.local.entities.MessageSyncStatus
import com.SE114.food_tracker.data.repository.ChatRepository
import com.SE114.food_tracker.feature.chat.components.MessageUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber


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
                loadGroupMembers(convId)
                refreshChatData(convId)
            }
        }
    }

    fun fetchConversationsFromServer(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                chatRepository.fetchAndSaveConversationsToLocal()
            } catch (e: Exception) {
                Timber.tag("Chat").e(e, "Failed to fetch conversations")
            } finally {
                onComplete()
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

    private var connectedConversationId: String? = null

    fun connectToConversation(conversationId: String) {
        connectedConversationId = conversationId
        viewModelScope.launch {
            chatRepository.subscribeToChatRealtime(conversationId)
            try {
                chatRepository.syncMessagesFromServer(conversationId)
                chatRepository.fetchGroupMembersFromServer(conversationId).also { members ->
                    _groupMembers.value = members
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // This VM is keyed per conversation (hiltViewModel(key = id)), so onCleared fires when
    // the chat is popped — the point to release its realtime channel and collectors.
    override fun onCleared() {
        super.onCleared()
        connectedConversationId?.let { chatRepository.unsubscribeFromChatRealtime(it) }
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

    fun updateGroupAvatar(
        conversationId: String,
        imageUri: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            onResult(chatRepository.updateGroupAvatar(conversationId, imageUri))
        }
    }

    fun removeGroupAvatar(conversationId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            onResult(chatRepository.removeGroupAvatar(conversationId))
        }
    }

    fun addMembers(conversationId: String, userIds: List<String>) {
        viewModelScope.launch {
            // Lấy danh sách mới nhất từ server trước khi lọc
            val currentRemoteMembers = chatRepository.fetchGroupMembersFromServer(conversationId)
            val currentMemberIds = currentRemoteMembers.map { it.first.lowercase().trim() }

            val newMembers = userIds.filter { it.lowercase().trim() !in currentMemberIds }

            if (newMembers.isNotEmpty()) {
                chatRepository.addMembersToGroup(conversationId, newMembers)
                // Cập nhật lại State sau khi thêm
                loadGroupMembers(conversationId)
            }
        }
    }

    fun kickGroupMember(conversationId: String, userId: String, name: String) {
        viewModelScope.launch {
            // 1. Thực hiện kick thành viên trên server trước
            chatRepository.kickMember(conversationId, userId, name)

            // 2. Kiểm tra nếu người bị kick là chính mình thì mới bắn event chuyển màn hình
            if (userId.lowercase() == currentUserId) {
                _navigationEvent.emit("LEFT")
            }
        }
    }
    fun deleteDirectChat(conversationId: String) {
        viewModelScope.launch {
            chatRepository.deleteOneToOneChat(conversationId)
            _navigationEvent.emit("LEFT")
        }
    }
    private val _navigationEvent = MutableSharedFlow<String?>()
    val navigationEvent = _navigationEvent.asSharedFlow()
    fun disbandGroup(conversationId: String) {
        viewModelScope.launch {
            if (isCurrentAdmin.value) {
                chatRepository.disbandGroup(conversationId)
                _navigationEvent.emit("DISBANDED")
            }
        }
    }

    fun getConversationState(conversationId: String): Flow<Conversation?> {
        return chatRepository.getLocalConversation(conversationId)
    }

    fun getMessagesState(conversationId: String): Flow<List<MessageUiModel>> {
        // The realtime connection is owned by ChatScreen's LaunchedEffect(conversationId);
        // connecting here too would fire a second concurrent subscribe for the same conversation.
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

    fun sendTextMessage(
        conversationId: String,
        text: String,
        isOneToOne: Boolean = false,
        friendUserId: String? = null
    ) {
        viewModelScope.launch {
            var targetConvId = conversationId
            if (isOneToOne && !friendUserId.isNullOrBlank()) {
                val existingId = chatRepository.getOrCreateOneToOneChat(friendUserId)
                if (existingId != null) targetConvId = existingId
            }
            if (targetConvId.isBlank()) {
                println("LỖI: Không lấy được targetConvId!")
                return@launch
            }
            chatRepository.sendMessage(targetConvId, currentUserId, body = text, imageUrl = null)
        }
    }

    fun sendImageMessage(conversationId: String, imageUri: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(
                conversationId,
                currentUserId,
                body = null,
                imageUrl = imageUri
            )
        }
    }

    fun retryFailedMessage(messageEntity: Message) {
        viewModelScope.launch { chatRepository.retryMessage(messageEntity) }
    }

    // ── CONVERSATION LIST ─────────────────────────────────────────────────────

    fun getConversationsWithUnreadFlow(): Flow<List<ConversationWithUnread>> {
        val userId = currentUserId
        return chatDAO.getAllConversationsWithUnread(userId)
    }

    fun getConversationsFlow(): Flow<List<Conversation>> {
        return chatDAO.getAllConversations()
    }

    /**
     * Open a 1-1 chat with a friend from the search suggestions. Resolves (or creates) the real
     * conversation on the server first, then hands back its id — sending to a conversation_id that
     * doesn't exist yet violates the message→conversation foreign key, which is why first messages
     * to a brand-new 1-1 used to fail.
     */
    fun openConversationWithFriend(
        friendUserId: String,
        friendName: String,
        onResolved: (conversationId: String, name: String) -> Unit
    ) {
        viewModelScope.launch {
            val conversationId = chatRepository.getOrCreateOneToOneChat(friendUserId)
            if (conversationId.isNullOrBlank()) {
                Timber.tag("Chat").e("Could not open 1-1 chat with %s", friendUserId)
                return@launch
            }
            onResolved(conversationId, friendName)
            // Pull the just-created conversation into Room so it shows in the list.
            fetchConversationsFromServer()
        }
    }

    // ── MARK AS READ ──────────────────────────────────────────────────────────

    fun markAsRead(conversationId: String) {
        viewModelScope.launch {
            chatRepository.markAsRead(conversationId)
        }
    }

    fun resetTransactionStatus() {
        isTransactionSuccess = null
    }

    suspend fun isCurrentUserAdmin(conversationId: String): Boolean =
        runCatching { chatRepository.isCurrentUserAdminOf(conversationId) }.getOrDefault(false)

    private fun formatToTime(timestamp: Long): String =
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))

    private fun formatToDate(timestamp: Long): String {
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()
        return if (target.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        ) "Hôm nay"
        else SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}