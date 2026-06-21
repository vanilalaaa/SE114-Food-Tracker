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
    var walletBalance by mutableStateOf(mockBalance)
        private set

    val walletTransactions: StateFlow<List<Map<String, kotlinx.serialization.json.JsonElement>>> =
        _mockWalletTransactions.asStateFlow()

    var isTransactionSuccess by mutableStateOf<Boolean?>(null)
        private set

    var isAlreadyReported by mutableStateOf(false)
        private set

    // KHỐI BỘ NHỚ TĨNH (COMPANION OBJECT) BẢO VỆ DỮ LIỆU KHÔNG BỊ RESET KHI THOÁT RA VÀO LẠI
    companion object {
        private var mockBalance by mutableStateOf(680000.0)
        private val _mockWalletTransactions =
            MutableStateFlow<List<Map<String, kotlinx.serialization.json.JsonElement>>>(
                listOf(
                    mapOf(
                        "id" to kotlinx.serialization.json.JsonPrimitive("tx_mồi_01"),
                        "type" to kotlinx.serialization.json.JsonPrimitive("deposit"),
                        "amount" to kotlinx.serialization.json.JsonPrimitive(200000.0),
                        "note" to kotlinx.serialization.json.JsonPrimitive("Nộp tiền quỹ cơm trưa tuần này"),
                        "created_at" to kotlinx.serialization.json.JsonPrimitive("10:15 - Hôm nay")
                    ),
                    mapOf(
                        "id" to kotlinx.serialization.json.JsonPrimitive("tx_mồi_02"),
                        "type" to kotlinx.serialization.json.JsonPrimitive("withdrawal"),
                        "amount" to kotlinx.serialization.json.JsonPrimitive(-120000.0),
                        "note" to kotlinx.serialization.json.JsonPrimitive("Mua cơm gà phi lê nhóm"),
                        "created_at" to kotlinx.serialization.json.JsonPrimitive("12:30 - Hôm qua")
                    )
                )
            )
    }

    init {
        // 1. Duy trì lệnh đồng bộ từ server như bình thường
        fetchConversationsFromServer()

        // 2. MỒI DATA VÀO ROOM LOCAL ĐỂ KIỂM TRA MÀN HÌNH
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

                // Đồng bộ biến UI khớp trực tiếp với bộ nhớ tĩnh Companion Object hiện thời
                walletBalance = mockBalance
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
        if (conversationId == "phong_test_114") {
            walletBalance = mockBalance
            return
        }

        viewModelScope.launch {
            walletBalance = chatRepository.getWalletBalance(conversationId)
            _mockWalletTransactions.value =
                chatRepository.fetchWalletTransactionsFromServer(conversationId)
        }
    }

    // Hàm xử lý kích hoạt giao dịch từ Giao diện gửi lên
    fun executeWalletTransaction(
        conversationId: String,
        amount: Double,
        isDeposit: Boolean,
        note: String
    ) {
        viewModelScope.launch {
            if (conversationId == "phong_test_114") {
                val finalAmount = if (isDeposit) amount else -amount
                mockBalance += finalAmount
                walletBalance = mockBalance

                val newMockTx = mapOf(
                    "id" to kotlinx.serialization.json.JsonPrimitive(UUID.randomUUID().toString()),
                    "type" to kotlinx.serialization.json.JsonPrimitive(if (isDeposit) "deposit" else "withdrawal"),
                    "amount" to kotlinx.serialization.json.JsonPrimitive(finalAmount),
                    "note" to kotlinx.serialization.json.JsonPrimitive(note),
                    "created_at" to kotlinx.serialization.json.JsonPrimitive("Vừa xong")
                )
                _mockWalletTransactions.value = listOf(newMockTx) + _mockWalletTransactions.value
                isTransactionSuccess = true
            } else {
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
    }

    fun resetTransactionStatus() {
        isTransactionSuccess = null
    }

    // ── BÁO CÁO (REPORT) ──

    // Kiểm tra xem reporter hiện tại đã từng gửi báo cáo cho mục tiêu này chưa để gài cảnh báo lên UI
    fun checkReportStatus(targetId: String) {
        viewModelScope.launch {
            isAlreadyReported = chatRepository.checkIfAlreadyReported(targetId)
        }
    }

    // Gửi báo cáo thật lên Supabase và nhận kết quả thông qua callback lambda để Toast
    fun sendReport(targetId: String, reason: String, note: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = chatRepository.submitReport(targetId, reason, note.ifBlank { null })
            onResult(success)
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