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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.flow.first

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

    @Serializable
    data class SupabaseParticipantDto(
        @SerialName("conversation_id") val conversationId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("is_admin") val isAdmin: Boolean
    )

    @Serializable
    data class SupabaseGroupWalletDto(
        @SerialName("id") val id: String,
        @SerialName("name") val name: String,
        @SerialName("balance") val balance: Double,
        @SerialName("created_by") val createdBy: String
    )

    @Serializable
    data class SupabaseWalletMembershipDto(
        @SerialName("wallet_id") val walletId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("role") val role: String
    )

    @Serializable
    data class WalletRpcArgs(
        @SerialName("wallet_id") val walletId: String,
        @SerialName("amount") val amount: Double,
        @SerialName("note") val note: String
    )

    @Serializable
    data class ProfileNameDto(
        @SerialName("display_name") val displayName: String? = null
    )

    @Serializable
    data class GroupMemberResponseDto(
        @SerialName("user_id") val userId: String,
        @SerialName("profiles") val profiles: ProfileNameDto? = null
    )

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
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
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:ssXXX", java.util.Locale.US)
                val date = sdfBackup.parse(serverTimeStr)
                date?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    fun getAuthenticatedUserId(): String {
        return supabaseClient.auth.currentUserOrNull()?.id?.lowercase() ?: ""
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
            if (currentUserId.isBlank()) {
                println("CẢNH BÁO: Không thể fetch phòng chat vì currentUserId đang trống!")
                return
            }

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
                        }.decodeSingle<SupabaseConversationDto>()

                    val localConversation = LocalConversation(
                        id = response.id,
                        name = response.name ?: "Trò chuyện 1-1",
                        isGroup = response.isGroup,
                        walletId = response.walletId ?: "wallet_default"
                    )
                    chatDAO.insertConversation(localConversation)
                    println("Đồng bộ thành công phòng chat local: ${response.name}")
                } catch (e: Exception) {
                    println("Lỗi bốc chi tiết phòng chat $convId: ${e.localizedMessage}")
                }
            }
        } catch (e: Exception) {
            println("Lỗi hệ thống luồng fetchAndSaveConversationsToLocal: ${e.localizedMessage}")
        }
    }

    // ── CHỨC NĂNG REALTIME CHANNEL ──

    fun subscribeToChatRealtime(conversationId: String) {
        repositoryScope.launch {
            if (activeChannels.containsKey(conversationId)) return@launch

            try {
                val channel = supabaseClient.channel("chat_channel_$conversationId")

                val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "message"
                }

                val conversationUpdateFlow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table = "conversation"
                }

                // Nhận tin nhắn mới từ cổng phát sóng (Tin nhắn Chat + Tin nhắn Hệ thống)
                repositoryScope.launch {
                    changeFlow.collect { action ->
                        val dto = action.decodeRecord<SupabaseMessageDto>()

                        // Đã sửa đổi: Bỏ chặn ID máy gửi để cả tin nhắn thường lẫn tin nhắn hệ thống đồng bộ mượt mà sang các máy khác nhau lập tức
                        if (dto.conversationId == conversationId) {
                            val exist = chatDAO.getMessageByServerId(dto.id ?: "")
                            if (exist == null) {
                                val incomingMessage = Message(
                                    localId = dto.id ?: UUID.randomUUID().toString(),
                                    serverId = dto.id,
                                    conversationId = dto.conversationId,
                                    senderId = dto.senderId.lowercase(),
                                    body = dto.body,
                                    imageUrl = dto.imageUrl,
                                    isSystem = dto.isSystem,
                                    syncStatus = MessageSyncStatus.SENT,
                                    createdAt = parseServerTimeToLong(dto.createdAt)
                                )
                                chatDAO.insertMessage(incomingMessage)
                                println("Realtime: Đã nhận live tin nhắn mới (Chat/System) thành công!")
                            }
                        }
                    }
                }

                // Nhận cập nhật phòng chat
                repositoryScope.launch {
                    conversationUpdateFlow.collect { action ->
                        val updatedDto = action.decodeRecord<SupabaseConversationDto>()
                        if (updatedDto.id == conversationId) {
                            val currentConv = chatDAO.getConversationById(conversationId).firstOrNull()
                            currentConv?.let {
                                chatDAO.insertConversation(
                                    it.copy(
                                        name = updatedDto.name ?: it.name,
                                        walletId = updatedDto.walletId ?: it.walletId
                                    )
                                )
                            }
                        }
                    }
                }

                channel.subscribe()
                activeChannels[conversationId] = channel
                println("Supabase Realtime: Đã mở cổng đồng bộ cho phòng $conversationId")
            } catch (e: Exception) {
                println("Supabase Realtime Lỗi kết nối ban đầu: ${e.localizedMessage}")
                handleRealtimeReconnect(conversationId)
            }
        }
    }

    // 🔥 ĐÃ THÊM: Đồng bộ luồng tạo nhóm và mời thành viên ra khỏi nhóm thời gian thực toàn cục
    fun subscribeToGlobalConversationsRealtime() {
        val currentUserId = getAuthenticatedUserId()
        if (currentUserId.isBlank()) return

        repositoryScope.launch {
            try {
                val globalChannel = supabaseClient.channel("global_conv_channel_$currentUserId")

                val participantChangeFlow = globalChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "conversation_participant"
                }

                repositoryScope.launch {
                    participantChangeFlow.collect { action ->
                        println("Realtime Global: Phát hiện thay đổi thành viên nhóm (Tạo nhóm / Mời ra). Đang làm mới danh sách...")
                        fetchAndSaveConversationsToLocal()
                    }
                }

                globalChannel.subscribe()
                println("Supabase Realtime: Đã kích hoạt lắng nghe biến động nhóm toàn cục thành công!")
            } catch (e: Exception) {
                println("Lỗi kích hoạt kênh toàn cục: ${e.localizedMessage}")
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
            senderId = senderId.lowercase(),
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
                                eq("user_id", friendUserId.lowercase())
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
                    "user_id" to friendUserId.lowercase(),
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

            val newConversation = SupabaseConversationDto(
                id = groupUuid,
                name = groupName,
                isGroup = true,
                walletId = null
            )

            supabaseClient.from("conversation").insert(newConversation)

            val currentUserId = getAuthenticatedUserId()
            val allMembers = (memberUserIds + currentUserId).map { it.lowercase() }.distinct()

            val participantRows = allMembers.map { userId ->
                SupabaseParticipantDto(
                    conversationId = groupUuid,
                    userId = userId,
                    isAdmin = userId == currentUserId
                )
            }
            supabaseClient.from("conversation_participant").insert(participantRows)

            val localGroup = LocalConversation(
                id = groupUuid,
                name = groupName,
                isGroup = true,
                walletId = "wallet_default"
            )
            chatDAO.insertConversation(localGroup)

            sendSystemMessage(groupUuid, "Nhóm '$groupName' đã được khởi tạo thành công.")
            return groupUuid
        } catch (e: Exception) {
            println("Lỗi hệ thống tạo nhóm: ${e.localizedMessage}")
            return null
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
                }.decodeSingle<Map<String, kotlinx.serialization.json.JsonElement>>()

            response["is_admin"]?.toString()?.replace("\"", "")?.toBoolean() ?: false
        } catch (e: Exception) {
            println("Lỗi kiểm tra quyền Admin: ${e.localizedMessage}")
            false
        }
    }

    suspend fun fetchGroupMembersFromServer(conversationId: String): List<Pair<String, String>> {
        return try {
            val response = supabaseClient.from("conversation_participant")
                .select(io.github.jan.supabase.postgrest.query.Columns.raw("user_id, profile(display_name)")) {
                    filter { eq("conversation_id", conversationId) }
                }.decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()

            response.map { row ->
                val userId = row["user_id"]?.toString()?.replace("\"", "")?.lowercase()?.trim() ?: ""

                val profileElement = row["profile"]
                val displayName = try {
                    if (profileElement != null) {
                        profileElement.jsonObject["display_name"]?.jsonPrimitive?.content
                            ?: "Thành viên"
                    } else {
                        "Thành viên"
                    }
                } catch (e: Exception) {
                    "Thành viên"
                }

                Pair(userId, displayName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Lỗi bốc danh sách thành viên thật từ server: ${e.localizedMessage}")
            emptyList()
        }
    }

    suspend fun updateGroupName(conversationId: String, newName: String) {
        try {
            // 1. Cập nhật tên mới cho nhóm chat trên bảng conversation của Server
            supabaseClient.from("conversation").update(mapOf("name" to newName)) {
                filter { eq("id", conversationId) }
            }

            val currentConv = chatDAO.getConversationById(conversationId).firstOrNull()

            val actualWalletId = currentConv?.walletId
            if (!actualWalletId.isNullOrBlank() && actualWalletId != "wallet_default") {
                supabaseClient.from("group_wallet").update(mapOf("name" to newName)) {
                    filter { eq("id", actualWalletId) }
                }
                println("Đồng bộ: Tên Quỹ nhóm đã được tự động đổi thành '$newName' theo tên nhóm chat!")
            }

            currentConv?.let {
                chatDAO.insertConversation(it.copy(name = newName))
            }

            sendSystemMessage(conversationId, "Tên nhóm đã được đổi thành '$newName'")
            println("Đổi tên nhóm thành công!")

        } catch (e: Exception) {
            println("Lỗi đổi tên nhóm và đồng bộ tên ví: ${e.localizedMessage}")
        }
    }

    suspend fun kickMember(conversationId: String, userIdToKick: String, memberName: String) {
        try {
            val currentAdminId = getAuthenticatedUserId()
            val actualGroupMembers = fetchGroupMembersFromServer(conversationId)
            val adminName = actualGroupMembers.find { it.first == currentAdminId }?.second ?: "Admin"

            supabaseClient.from("conversation_participant").delete {
                filter {
                    eq("conversation_id", conversationId)
                    eq("user_id", userIdToKick.lowercase())
                }
            }

            fetchAndSaveConversationsToLocal()

            sendSystemMessage(conversationId, "$adminName đã mời $memberName rời khỏi nhóm.")

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
        memberUserIds: List<String>
    ): Boolean {
        return try {
            val walletUuid = java.util.UUID.randomUUID().toString()
            val currentUserId = getAuthenticatedUserId()

            if (currentUserId.isBlank()) {
                println("Lỗi tạo ví: Không tìm thấy ID User hợp lệ")
                return false
            }

            val currentConv = chatDAO.getConversationById(conversationId).first()
            val walletName = currentConv?.name ?: "Quỹ nhóm"

            val walletDto = SupabaseGroupWalletDto(
                id = walletUuid,
                name = walletName,
                balance = 0.0,
                createdBy = currentUserId
            )
            supabaseClient.from("group_wallet").insert(walletDto)

            val allMembers = (memberUserIds + currentUserId).map { it.lowercase() }.distinct()
            val membershipRows = allMembers.map { userId ->
                SupabaseWalletMembershipDto(
                    walletId = walletUuid,
                    userId = userId,
                    role = if (userId == currentUserId) "owner" else "member"
                )
            }
            supabaseClient.from("wallet_membership").insert(membershipRows)

            val updateData = mapOf("wallet_id" to walletUuid)
            supabaseClient.from("conversation").update(updateData) {
                filter { eq("id", conversationId) }
            }

            currentConv?.let {
                chatDAO.insertConversation(it.copy(walletId = walletUuid))
            }

            sendSystemMessage(
                conversationId,
                "Quỹ nhóm '$walletName' đã được thiết lập thành công."
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            println("Lỗi luồng tạo ví quỹ nhóm thực tế: ${e.localizedMessage}")
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
        txType: String,
        note: String,
        itemId: String? = null
    ): Boolean {
        return try {
            val currentUserId = getAuthenticatedUserId()
            val conversation = chatDAO.getConversationById(conversationId).first()
            val walletId = conversation?.walletId ?: ""

            if (walletId.isBlank() || walletId == "wallet_default") {
                println("Lỗi Repository: Không tìm thấy Wallet ID hợp lệ")
                return false
            }

            val actualGroupMembers = fetchGroupMembersFromServer(conversationId)
            val currentUserName = actualGroupMembers.find { it.first == currentUserId }?.second ?: "Thành viên"

            val rpcArgs = WalletRpcArgs(
                walletId = walletId,
                amount = amount,
                note = note
            )

            val messageText = if (txType == "deposit") {
                "Hệ thống: ${currentUserName} đã nộp ${String.format("%,.0f", amount)} VND vào quỹ nhóm. Nội dung: $note"
            } else {
                "Hệ thống: ${currentUserName} đã rút ${String.format("%,.0f", amount)} VND từ quỹ nhóm. Nội dung: $note"
            }

            when (txType) {
                "deposit" -> {
                    supabaseClient.postgrest.rpc(
                        function = "rpc_wallet_deposit",
                        parameters = rpcArgs
                    )
                    sendMessage(
                        conversationId = conversationId,
                        senderId = "system",
                        body = messageText,
                        imageUrl = null,
                        isSystem = true
                    )
                }

                "withdrawal" -> {
                    supabaseClient.postgrest.rpc(
                        function = "rpc_wallet_withdraw",
                        parameters = rpcArgs
                    )
                    sendMessage(
                        conversationId = conversationId,
                        senderId = "system",
                        body = messageText,
                        imageUrl = null,
                        isSystem = true
                    )
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
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

    suspend fun syncMessagesFromServer(conversationId: String) {
        try {
            val remoteMessages = supabaseClient.from("message")
                .select {
                    filter { eq("conversation_id", conversationId) }
                }.decodeList<SupabaseMessageDto>()

            remoteMessages.forEach { dto ->
                val existingLocalMessage = dto.id?.let { chatDAO.getMessageByServerId(it) }

                if (existingLocalMessage != null) {
                    val updatedMessage = existingLocalMessage.copy(
                        serverId = dto.id,
                        body = dto.body,
                        imageUrl = dto.imageUrl ?: existingLocalMessage.imageUrl,
                        syncStatus = MessageSyncStatus.SENT,
                        createdAt = parseServerTimeToLong(dto.createdAt)
                    )
                    chatDAO.updateMessage(updatedMessage)
                } else {
                    val newLocalMessage = Message(
                        localId = dto.id ?: UUID.randomUUID().toString(),
                        serverId = dto.id,
                        conversationId = dto.conversationId,
                        senderId = dto.senderId.lowercase(),
                        body = dto.body,
                        imageUrl = dto.imageUrl,
                        isSystem = dto.isSystem,
                        syncStatus = MessageSyncStatus.SENT,
                        createdAt = parseServerTimeToLong(dto.createdAt)
                    )
                    chatDAO.insertMessage(newLocalMessage)
                }
            }
            println("Đồng bộ thông minh hoàn tất! Toàn bộ link ảnh quá khứ đã được găm chặt xuống Room.")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Lỗi luồng đồng bộ: ${e.localizedMessage}")
        }
    }

    private suspend fun performNetworkSend(message: Message) {
        try {
            var finalImageUrl = message.imageUrl
            val currentUserId = getAuthenticatedUserId()

            if (message.imageUrl != null && (message.imageUrl.startsWith("content://") || message.imageUrl.startsWith("file://"))) {
                try {
                    val uri = android.net.Uri.parse(message.imageUrl)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val fileBytes = inputStream?.use { it.readBytes() }

                    if (fileBytes != null) {
                        val storageBucket = supabaseClient.storage.from("chat-images")
                        val fileName = "${java.util.UUID.randomUUID()}.jpg"

                        storageBucket.upload(path = fileName, data = fileBytes) {
                            upsert = true
                        }

                        finalImageUrl = storageBucket.publicUrl(fileName)
                        println("Supabase Storage: Upload thành công link thật: $finalImageUrl")
                    }
                } catch (storageErr: Exception) {
                    println("Lỗi upload ảnh lên Storage: ${storageErr.localizedMessage}")
                }
            }

            val finalSenderId = if (message.isSystem || message.senderId == "system") "system" else currentUserId

            val messageDto = SupabaseMessageDto(
                conversationId = message.conversationId,
                senderId = finalSenderId,
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
                senderId = finalSenderId,
                createdAt = parseServerTimeToLong(insertedDto.createdAt)
            )
            chatDAO.updateMessage(successMessage)

        } catch (e: Exception) {
            e.printStackTrace()
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