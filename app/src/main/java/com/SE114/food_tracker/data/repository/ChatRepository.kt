package com.SE114.food_tracker.data.repository

import android.content.Context
import android.net.Uri
import com.SE114.food_tracker.data.local.dao.ChatDAO
import com.SE114.food_tracker.data.local.entities.Message
import com.SE114.food_tracker.data.local.entities.MessageSyncStatus
import com.SE114.food_tracker.data.local.entities.Conversation as LocalConversation
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.postgrest

@Singleton
class ChatRepository @Inject constructor(
    private val chatDAO: ChatDAO,
    private val supabaseClient: SupabaseClient,
    @ApplicationContext private val context: Context
) {
    @Serializable
    data class SupabaseMessageDto(
        @SerialName("id") val id: String? = null,
        @SerialName("conversation_id") val conversationId: String,
        @SerialName("sender_id") val senderId: String,
        @SerialName("body") val body: String?,
        @SerialName("image_url") val imageUrl: String?,
        @SerialName("is_system") val isSystem: Boolean,
        @SerialName("created_at") val createdAt: String? = null
    )

    @Serializable
    data class SupabaseConversationDto(
        @SerialName("id") val id: String,
        @SerialName("name") val name: String? = null,
        @SerialName("is_group") val isGroup: Boolean = false,
        @SerialName("wallet_id") val walletId: String? = null
    )

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Quản lý danh sách các phòng chat đang kết nối realtime bằng Map thay vì biến đơn
    private val activeChannels = mutableMapOf<String, RealtimeChannel>()

    private fun parseServerTimeToLong(serverTimeStr: String?): Long {
        if (serverTimeStr.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            val sdf =
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", java.util.Locale.US)
            val date = sdf.parse(serverTimeStr)
            date?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                val sdfBackup =
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
                val date = sdfBackup.parse(serverTimeStr)
                date?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    fun getAuthenticatedUserId(): String {
        return supabaseClient.auth.currentUserOrNull()?.id ?: "vy_id"
    }

    fun getMessagesStream(conversationId: String): Flow<List<Message>> {
        return chatDAO.getMessagesByConversation(conversationId)
    }

    fun getLocalConversation(conversationId: String): Flow<LocalConversation?> {
        return chatDAO.getConversationById(conversationId)
    }

    suspend fun fetchAndSaveConversationsToLocal() {
        try {
            val currentUserId = getAuthenticatedUserId()
            val myParticipations = supabaseClient.from("conversation_participant")
                .select {
                    filter { eq("user_id", currentUserId) }
                }.decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()

            myParticipations.forEach { part ->
                val convId =
                    part["conversation_id"]?.toString()?.replace("\"", "") ?: return@forEach
                try {
                    val response = supabaseClient.from("conversation")
                        .select {
                            filter { eq("id", convId) }
                        }.decodeSingle<Map<String, kotlinx.serialization.json.JsonElement>>()

                    val cName = response["name"]?.toString()?.replace("\"", "")?.let {
                        if (it == "null") null else it
                    }
                    val cIsGroup = response["is_group"]?.toString()?.toBoolean() ?: false
                    val cWalletId = response["wallet_id"]?.toString()?.replace("\"", "")?.let {
                        if (it == "null") null else it
                    }

                    val localConversation = LocalConversation(
                        id = convId,
                        name = cName ?: "Trò chuyện 1-1",
                        isGroup = cIsGroup,
                        walletId = cWalletId ?: "wallet_default"
                    )
                    chatDAO.insertConversation(localConversation)
                } catch (e: Exception) {
                    println("Lỗi bốc chi tiết phòng chat $convId: ${e.localizedMessage}")
                }
            }
        } catch (e: Exception) {
            println("Lỗi hệ thống luồng fetchAndSaveConversationsToLocal: ${e.localizedMessage}")
        }
    }

    // ── CHỨC NĂNG REALTIME CHANNEL (SUBSCRIBE + RECONNECT) ──

    fun subscribeToChatRealtime(conversationId: String) {
        repositoryScope.launch {
            if (activeChannels.containsKey(conversationId)) return@launch

            try {
                val channel = supabaseClient.channel("chat_channel_$conversationId")
                val changeFlow =
                    channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                        table = "message"
                    }

                repositoryScope.launch {
                    changeFlow.collect { action ->
                        val dto = action.decodeRecord<SupabaseMessageDto>()
                        val currentUserId = getAuthenticatedUserId()

                        // Kiểm tra tin nhắn có thuộc về phòng chat này hay không
                        if (dto.conversationId == conversationId && dto.senderId != currentUserId) {
                            val incomingMessage = Message(
                                localId = dto.id ?: UUID.randomUUID().toString(),
                                serverId = dto.id,
                                conversationId = dto.conversationId,
                                senderId = dto.senderId,
                                body = dto.body,
                                imageUrl = dto.imageUrl,
                                isSystem = dto.isSystem,
                                syncStatus = MessageSyncStatus.SENT,
                                createdAt = parseServerTimeToLong(dto.createdAt)
                            )
                            chatDAO.insertMessage(incomingMessage)
                        }
                    }
                }

                channel.subscribe()
                activeChannels[conversationId] = channel
                println("Supabase Realtime: Đã mở kênh nghe động độc quyền cho phòng $conversationId")
            } catch (e: Exception) {
                println("Supabase Realtime Lỗi kết nối ban đầu: ${e.localizedMessage}")
                handleRealtimeReconnect(conversationId)
            }
        }
    }

    private suspend fun handleRealtimeReconnect(conversationId: String) {
        var isConnected = false
        var retryDelay = 2000L

        while (!isConnected) {
            delay(retryDelay)
            try {
                val channel = activeChannels[conversationId]
                    ?: supabaseClient.channel("chat_channel_$conversationId")
                channel.subscribe()
                activeChannels[conversationId] = channel
                isConnected = true
                println("Supabase Realtime: Reconnect thành công phòng $conversationId!")
            } catch (e: Exception) {
                retryDelay = (retryDelay * 2).coerceAtMost(60000L)
            }
        }
    }

    // ── CHỨC NĂNG CHAT NHÓM & QUẢN TRỊ VIÊN ──

    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        body: String?,
        imageUrl: String?,
        isSystem: Boolean = false
    ) {
        val localId = UUID.randomUUID().toString()
        val pendingMessage = Message(
            localId = localId,
            serverId = null,
            conversationId = conversationId,
            senderId = senderId,
            body = body,
            imageUrl = imageUrl,
            isSystem = isSystem,
            syncStatus = MessageSyncStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
        chatDAO.insertMessage(pendingMessage)
        performNetworkSend(pendingMessage)
    }

    suspend fun getOrCreateOneToOneChat(friendUserId: String): String? {
        return try {
            val currentUserId = getAuthenticatedUserId()
            val myParticipations = supabaseClient.from("conversation_participant")
                .select {
                    filter { eq("user_id", currentUserId) }
                }.decodeList<Map<String, String>>()

            var existingConversationId: String? = null
            for (part in myParticipations) {
                val convId = part["conversation_id"] ?: continue
                val isGroupCheck = supabaseClient.from("conversation")
                    .select {
                        filter { eq("id", convId) }
                    }.decodeSingle<SupabaseConversationDto>().isGroup

                if (!isGroupCheck) {
                    val friendCheck = supabaseClient.from("conversation_participant")
                        .select {
                            filter {
                                eq("conversation_id", convId)
                                eq("user_id", friendUserId)
                            }
                        }.decodeList<Map<String, String>>()

                    if (friendCheck.isNotEmpty()) {
                        existingConversationId = convId
                        break
                    }
                }
            }

            if (existingConversationId != null) return existingConversationId

            val newChatUuid = UUID.randomUUID().toString()
            supabaseClient.from("conversation")
                .insert(mapOf("id" to newChatUuid, "is_group" to false, "name" to null))

            val participantRows = listOf(
                mapOf(
                    "conversation_id" to newChatUuid,
                    "user_id" to currentUserId,
                    "is_admin" to false
                ),
                mapOf(
                    "conversation_id" to newChatUuid,
                    "user_id" to friendUserId,
                    "is_admin" to false
                )
            )
            supabaseClient.from("conversation_participant").insert(participantRows)
            newChatUuid
        } catch (e: Exception) {
            println("Lỗi luồng tạo chat 1-1: ${e.localizedMessage}")
            null
        }
    }

    suspend fun createGroupChat(groupName: String, memberUserIds: List<String>): String? {
        return try {
            val groupUuid = UUID.randomUUID().toString()

            // Tạo phòng nhóm ban đầu (Chưa kích hoạt Ví Quỹ theo đúng kịch bản nút bấm riêng biệt)
            val conversationMap = mapOf(
                "id" to groupUuid,
                "is_group" to true,
                "name" to groupName,
                "wallet_id" to null
            )
            supabaseClient.from("conversation").insert(conversationMap)

            val currentUserId = getAuthenticatedUserId()
            val allMembers = (memberUserIds + currentUserId).distinct()
            val participantRows = allMembers.map { userId ->
                mapOf(
                    "conversation_id" to groupUuid,
                    "user_id" to userId,
                    "is_admin" to (userId == currentUserId)
                )
            }
            supabaseClient.from("conversation_participant").insert(participantRows)

            sendSystemMessage(groupUuid, "Hệ thống: Nhóm '$groupName' đã được khởi tạo thành công.")
            groupUuid
        } catch (e: Exception) {
            println("Lỗi tạo nhóm: ${e.localizedMessage}")
            null
        }
    }

    suspend fun isCurrentUserAdminOf(conversationId: String): Boolean {
        return try {
            val currentUserId = getAuthenticatedUserId()
            val response = supabaseClient.from("conversation_participant")
                .select {
                    filter {
                        eq("conversation_id", conversationId)
                        eq("user_id", currentUserId)
                    }
                }.decodeSingle<Map<String, Boolean>>()
            response["is_admin"] ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateGroupName(conversationId: String, newName: String) {
        try {
            supabaseClient.from("conversation").update(mapOf("name" to newName)) {
                filter { eq("id", conversationId) }
            }
            sendSystemMessage(conversationId, "Hệ thống: Tên nhóm đã đổi thành '$newName'")
        } catch (e: Exception) {
            println("Lỗi đổi tên: ${e.localizedMessage}")
        }
    }

    suspend fun kickMember(conversationId: String, userIdToKick: String, memberName: String) {
        try {
            supabaseClient.from("conversation_participant").delete {
                filter {
                    eq("conversation_id", conversationId)
                    eq("user_id", userIdToKick)
                }
            }
            sendSystemMessage(conversationId, "Hệ thống: Admin đã mời $memberName rời khỏi nhóm.")
        } catch (e: Exception) {
            println("Lỗi kích thành viên: ${e.localizedMessage}")
        }
    }

    suspend fun sendSystemMessage(conversationId: String, content: String) {
        sendMessage(
            conversationId = conversationId,
            senderId = "system",
            body = content,
            imageUrl = null,
            isSystem = true
        )
    }

    suspend fun createGroupWalletForExistingChat(
        conversationId: String,
        walletName: String,
        memberUserIds: List<String>
    ): Boolean {
        return try {
            val walletUuid = "wallet_${UUID.randomUUID().toString().take(8)}"
            val currentUserId = getAuthenticatedUserId()

            // 1. Tạo bản ghi ví mới trong group_wallet (balance ban đầu = 0)
            supabaseClient.from("group_wallet").insert(
                mapOf(
                    "id" to walletUuid,
                    "name" to walletName,
                    "balance" to 0.0,
                    "created_by" to currentUserId
                )
            )

            // 2. Thiết lập wallet_membership cho tất cả thành viên (role='owner' cho Admin, 'member' cho còn lại)
            val allMembers = (memberUserIds + currentUserId).distinct()
            val membershipRows = allMembers.map { userId ->
                mapOf(
                    "wallet_id" to walletUuid,
                    "user_id" to userId,
                    "role" to if (userId == currentUserId) "owner" else "member"
                )
            }
            supabaseClient.from("wallet_membership").insert(membershipRows)

            // 3. Cập nhật liên kết khóa ngoại conversation.wallet_id = id của quỹ vừa tạo
            supabaseClient.from("conversation").update(mapOf("wallet_id" to walletUuid)) {
                filter { eq("id", conversationId) }
            }

            sendSystemMessage(
                conversationId,
                "Hệ thống: Quỹ nhóm '$walletName' đã được Admin thiết lập thành công."
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getWalletBalance(conversationId: String): Double {
        return try {
            val convResponse = supabaseClient.from("conversation")
                .select { filter { eq("id", conversationId) } }
                .decodeSingle<SupabaseConversationDto>()

            val walletId = convResponse.walletId ?: return 0.0

            val walletResponse = supabaseClient.from("group_wallet")
                .select { filter { eq("id", walletId) } }
                .decodeSingle<Map<String, kotlinx.serialization.json.JsonElement>>()

            walletResponse["balance"]?.toString()?.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            println("Lỗi bốc số dư ví thật từ server: ${e.localizedMessage}")
            0.0
        }
    }

    suspend fun executeWalletTransaction(
        conversationId: String,
        amount: Double,
        txType: String, // Nhận diện rõ ràng: "deposit", "withdrawal", "purchase"
        note: String,
        itemId: String? = null
    ): Boolean {
        return try {
            val convResponse = supabaseClient.from("conversation")
                .select { filter { eq("id", conversationId) } }
                .decodeSingle<SupabaseConversationDto>()
            val walletId = convResponse.walletId ?: return false

            when (txType) {
                "deposit" -> {
                    supabaseClient.postgrest.rpc(
                        function = "rpc_wallet_deposit",
                        parameters = mapOf(
                            "p_wallet_id" to walletId,
                            "p_amount" to amount,
                            "p_note" to note
                        )
                    )
                    sendSystemMessage(
                        conversationId,
                        "Hệ thống: Thành viên đã nộp ${
                            String.format(
                                "%,.0f",
                                amount
                            )
                        }đ vào quỹ nhóm. Nội dung: $note"
                    )
                }

                "withdrawal" -> {
                    supabaseClient.postgrest.rpc(
                        function = "rpc_wallet_withdraw",
                        parameters = mapOf(
                            "p_wallet_id" to walletId,
                            "p_amount" to amount,
                            "p_note" to note
                        )
                    )
                    sendSystemMessage(
                        conversationId,
                        "Hệ thống: Admin đã rút ${
                            String.format(
                                "%,.0f",
                                amount
                            )
                        }đ từ quỹ nhóm. Nội dung: $note"
                    )
                }

                "purchase" -> {
                    supabaseClient.postgrest.rpc(
                        function = "rpc_wallet_purchase",
                        parameters = mapOf(
                            "p_wallet_id" to walletId,
                            "p_amount" to amount,
                            "p_note" to note,
                            "p_item_id" to (itemId ?: "")
                        )
                    )
                    sendSystemMessage(
                        conversationId,
                        "Hệ thống: Quỹ nhóm đã chi tiêu ${
                            String.format(
                                "%,.0f",
                                amount
                            )
                        }đ để mua món công vụ. Nội dung: $note"
                    )
                }
            }
            true
        } catch (e: Exception) {
            println("Lỗi xử lý luồng giao dịch RPC: ${e.localizedMessage}")
            false
        }
    }

    suspend fun fetchWalletTransactionsFromServer(conversationId: String): List<Map<String, kotlinx.serialization.json.JsonElement>> {
        return try {
            val convResponse = supabaseClient.from("conversation")
                .select { filter { eq("id", conversationId) } }
                .decodeSingle<SupabaseConversationDto>()
            val walletId = convResponse.walletId ?: return emptyList()

            // Thực hiện query sắp xếp DESC theo thời gian tạo như BA quy định
            supabaseClient.from("wallet_transaction")
                .select {
                    filter { eq("wallet_id", walletId) }
                    order(column = "created_at", order = Order.DESCENDING)
                }.decodeList()
        } catch (e: Exception) {
            println("Lỗi lấy lịch sử giao dịch từ server: ${e.localizedMessage}")
            emptyList()
        }
    }

    // ── CHỨC NĂNG GỬI MẠNG LƯỚI & RETRY TIN NHẮN LỖI ──

    private suspend fun performNetworkSend(message: Message) {
        try {
            var finalImageUrl = message.imageUrl

            if (message.imageUrl != null && message.imageUrl.startsWith("content://")) {
                val uri = Uri.parse(message.imageUrl)
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileBytes = inputStream?.use { it.readBytes() }

                if (fileBytes != null) {
                    val storageBucket = supabaseClient.storage.from("chat-images")
                    val fileName = "${message.localId}.jpg"

                    storageBucket.upload(path = fileName, data = fileBytes) {
                        upsert = true
                    }
                    finalImageUrl = storageBucket.publicUrl(fileName)
                }
            }

            val messageDto = SupabaseMessageDto(
                conversationId = message.conversationId,
                senderId = message.senderId,
                body = message.body,
                imageUrl = finalImageUrl,
                isSystem = message.isSystem
            )

            val response = supabaseClient.from("message").insert(listOf(messageDto)) { select() }
            val insertedDto = response.decodeSingle<SupabaseMessageDto>()

            val successMessage = message.copy(
                syncStatus = MessageSyncStatus.SENT,
                serverId = insertedDto.id,
                imageUrl = finalImageUrl,
                createdAt = parseServerTimeToLong(insertedDto.createdAt)
            )
            chatDAO.updateMessage(successMessage)

        } catch (e: Exception) {
            println("Gửi tin thất bại, chuyển trạng thái FAILED: ${e.localizedMessage}")
            val failedMessage = message.copy(syncStatus = MessageSyncStatus.FAILED)
            chatDAO.updateMessage(failedMessage)
        }
    }

    suspend fun retryMessage(message: Message) {
        val reloadingMessage = message.copy(syncStatus = MessageSyncStatus.PENDING)
        chatDAO.updateMessage(reloadingMessage)
        performNetworkSend(reloadingMessage)
    }
}