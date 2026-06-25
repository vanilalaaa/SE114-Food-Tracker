package com.SE114.food_tracker.data.repository

import android.content.Context
import android.net.Uri
import com.SE114.food_tracker.data.local.dao.ChatDAO
import com.SE114.food_tracker.data.local.dao.MessageWithProfile
import com.SE114.food_tracker.data.local.entities.Message
import com.SE114.food_tracker.data.local.entities.MessageSyncStatus
import com.SE114.food_tracker.data.local.entities.UserProfileCacheEntity
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
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.BufferOverflow

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
    data class PurchaseRpcArgs(
        @SerialName("p_wallet_id") val walletId: String,
        @SerialName("p_amount")    val amount: Double,
        @SerialName("p_note")      val note: String,
        @SerialName("p_item_id")   @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
        val itemId: String? = null
    )

    /** Wallet info visible to the current user, with their role. */
    data class WalletWithRole(
        val walletId: String,
        val walletName: String,
        val role: String
    )

    @Serializable
    data class ProfileNameDto(
        @SerialName("display_name") val displayName: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null
    )

    @Serializable
    data class GroupMemberResponseDto(
        @SerialName("user_id") val userId: String,
        @SerialName("profiles") val profiles: ProfileNameDto? = null
    )

    @Serializable
    data class SupabaseProfileDto(
        @SerialName("id") val id: String,
        @SerialName("display_name") val displayName: String,
        @SerialName("avatar_url") val avatarUrl: String? = null
    )

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private val activeChannels = mutableMapOf<String, RealtimeChannel>()
    private val reconnectingChannels = mutableSetOf<String>()
    private val _memberUpdateSignal = kotlinx.coroutines.flow.MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val memberUpdateSignal = _memberUpdateSignal.asSharedFlow()
    private val _walletUpdateSignal = kotlinx.coroutines.flow.MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val walletUpdateSignal = _walletUpdateSignal.asSharedFlow()

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

    // 🔥 ĐÃ ĐỔI: Trỏ thẳng luồng tin nhắn sang câu lệnh JOIN kèm profile lót sẵn dưới máy
    fun getMessagesWithProfileStream(conversationId: String): Flow<List<MessageWithProfile>> {
        return chatDAO.getMessagesWithProfileStream(conversationId)
    }

    fun getLocalConversation(conversationId: String): Flow<LocalConversation?> {
        return chatDAO.getConversationById(conversationId)
    }

    suspend fun fetchAndSaveConversationsToLocal() {
        try {
            val currentUserId = getAuthenticatedUserId()
            if (currentUserId.isBlank()) return

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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── CHỨC NĂNG REALTIME CHANNEL ──

    fun subscribeToChatRealtime(conversationId: String) {
        repositoryScope.launch {
            if (activeChannels.containsKey(conversationId)) return@launch

            try {
                val channel = supabaseClient.channel("chat_channel_$conversationId")

                val changeFlow =
                    channel.postgresChangeFlow<PostgresAction.Insert>(
                        schema = "public"
                    ) {
                        table = "message"
                        filter("conversation_id", FilterOperator.EQ, conversationId)
                    }

                val conversationUpdateFlow =
                    channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                        table = "conversation"
                    }

                val walletUpdateFlow =
                    channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                        table = "group_wallet"
                    }

                val transactionInsertFlow =
                    channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                        table = "wallet_transaction"
                    }

                val participantInsert =
                    channel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Insert>(
                        schema = "public"
                    ) {
                        table = "conversation_participant"
                    }
                val participantDelete =
                    channel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Delete>(
                        schema = "public"
                    ) {
                        table = "conversation_participant"
                    }

                // FIX: subscribe() MUST come before launching collectors.
                // Collectors registered on a channel that hasn't subscribed yet miss events
                // that arrive during the WebSocket handshake, causing the ~8-10s delay.
                channel.subscribe()
                activeChannels[conversationId] = channel
                println("Supabase Realtime: Đã mở cổng đồng bộ cho phòng $conversationId")

                // Bắn tín hiệu nạp lại thành viên khi có người vào
                repositoryScope.launch {
                    participantInsert.collect {
                        _memberUpdateSignal.tryEmit(conversationId)
                    }
                }

                // Bắn tín hiệu nạp lại thành viên khi có người ra
                repositoryScope.launch {
                    participantDelete.collect {
                        _memberUpdateSignal.tryEmit(conversationId)
                    }
                }
                repositoryScope.launch {
                    changeFlow.collect { action ->
                        val dto = action.decodeRecord<SupabaseMessageDto>()
                        // conversationId filter is now enforced server-side; no client-side
                        // if-check needed, but kept as a cheap safety guard.
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
                                println("Realtime: Nhận live thành công tin nhắn mới: ${dto.body}")
                            }
                        }
                    }
                }

                repositoryScope.launch {
                    conversationUpdateFlow.collect { action ->
                        val updatedDto = action.decodeRecord<SupabaseConversationDto>()
                        if (updatedDto.id == conversationId) {
                            val currentConv =
                                chatDAO.getConversationById(conversationId).firstOrNull()
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

                repositoryScope.launch {
                    walletUpdateFlow.collect {

                        _walletUpdateSignal.tryEmit(conversationId)
                    }
                }

                repositoryScope.launch {
                    transactionInsertFlow.collect {

                        _walletUpdateSignal.tryEmit(conversationId)
                    }
                }
            } catch (e: Exception) {
                println("Supabase Realtime Lỗi kết nối ban đầu: ${e.localizedMessage}")
                handleRealtimeReconnect(conversationId)
            }
        }
    }

    fun subscribeToGlobalConversationsRealtime() {
        val currentUserId = getAuthenticatedUserId()
        if (currentUserId.isBlank()) return

        repositoryScope.launch {
            try {
                val globalChannel = supabaseClient.channel("global_conv_channel_$currentUserId")

                val insertFlow =
                    globalChannel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Insert>(
                        schema = "public"
                    ) {
                        table = "conversation_participant"
                    }

                val deleteFlow =
                    globalChannel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Delete>(
                        schema = "public"
                    ) {
                        table = "conversation_participant"
                    }

                repositoryScope.launch {
                    insertFlow.collect { action ->

                        val userIdStr =
                            action.record["user_id"]?.toString()?.replace("\"", "")?.lowercase()
                        if (userIdStr == currentUserId) {
                            println("Realtime Global: Được mời vào nhóm mới!")
                            fetchAndSaveConversationsToLocal()
                        }
                    }
                }

                repositoryScope.launch {
                    deleteFlow.collect { action ->

                        val userIdStr = action.oldRecord["user_id"]?.toString()?.replace("\"", "")?.lowercase()
                        val convIdStr = action.oldRecord["conversation_id"]?.toString()?.replace("\"", "")

                        if (userIdStr == currentUserId) {
                            println("Realtime Global: Bị kick khỏi nhóm!")

                            if (convIdStr != null) {
                                chatDAO.deleteConversationById(convIdStr)
                            }
                            fetchAndSaveConversationsToLocal()
                        }
                    }
                }

                globalChannel.subscribe()
                println("Supabase Realtime: Kích hoạt lắng nghe biến động nhóm toàn cục hoàn tất!")
            } catch (e: Exception) {
                println("Lỗi kích hoạt kênh toàn cục: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun handleRealtimeReconnect(conversationId: String) {
        // Tránh tạo ra nhiều vòng lặp reconnect chồng chéo cho cùng 1 phòng chat
        if (reconnectingChannels.contains(conversationId)) return
        reconnectingChannels.add(conversationId)

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
                reconnectingChannels.remove(conversationId)
                println("Supabase Realtime: Đã kết nối lại thành công phòng $conversationId")
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
        println("DEBUG_TRACE: Đã bắt đầu vào hàm!")
        return try {
            val currentUserId = getAuthenticatedUserId()

            // 1. Kiểm tra định dạng UUID ngay đầu hàm
            // Một UUID chuẩn có độ dài 36 ký tự. Nếu friendUserId ngắn hơn, chắc chắn là Nickname.
            if (friendUserId.length < 36) {
                println("LỖI: Bạn đang truyền Nickname '$friendUserId' thay vì UUID. Phải lấy UUID từ memberList!")
                return null
            }

            val targetFriendId = friendUserId.lowercase()

            // 2. Tìm cuộc hội thoại đã tồn tại
            val myParticipations = supabaseClient.from("conversation_participant")
                .select { filter { eq("user_id", currentUserId) } }
                .decodeList<SupabaseParticipantDto>()

            val myConvIds = myParticipations.map { it.conversationId }

            if (myConvIds.isNotEmpty()) {
                val potentialParticipants = supabaseClient.from("conversation_participant")
                    .select { filter { eq("user_id", targetFriendId) } }
                    .decodeList<SupabaseParticipantDto>()

                for (part in potentialParticipants) {
                    if (myConvIds.contains(part.conversationId)) {
                        val conv = supabaseClient.from("conversation")
                            .select { filter { eq("id", part.conversationId) } }
                            .decodeSingleOrNull<SupabaseConversationDto>()

                        if (conv != null && !conv.isGroup) return part.conversationId
                    }
                }
            }

            // 3. Tạo mới với gen_random_uuid() ở server (nếu Supabase hỗ trợ)
            // Hoặc cứ dùng UUID.randomUUID().toString()
            val newChatUuid = UUID.randomUUID().toString()

            println("DEBUG_: Đang insert conv với UUID: $newChatUuid và user: $targetFriendId")

            supabaseClient.from("conversation").insert(mapOf(
                "id" to newChatUuid,
                "is_group" to false,
                "name" to null
            ))
            // ÉP KIỂM TRA NGAY TẠI CHỖ
            val verify = supabaseClient.from("conversation").select { filter { eq("id", newChatUuid) } }.decodeSingleOrNull<SupabaseConversationDto>()
            if (verify == null) {
                println("DEBUG_CRITICAL: Insert xong nhưng DB không tìm thấy ID vừa insert!")
            } else {
                println("DEBUG: Insert thành công, đã thấy ID trong DB")
            }
            supabaseClient.from("conversation_participant").insert(listOf(
                SupabaseParticipantDto(newChatUuid, currentUserId, false),
                SupabaseParticipantDto(newChatUuid, targetFriendId, false)
            ))

            return newChatUuid
        } catch (e: Exception) {
            println("LỖI CHI TIẾT TẠO CHAT: ${e.message}")
            e.printStackTrace()
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
            false
        }
    }

    // 🔥 ĐÃ FIX ĐỒNG BỘ MẠNG: Kéo dữ liệu về một phát là găm chặt đè thẳng xuống Room Local Cache để bốc xài lập tức
    suspend fun fetchGroupMembersFromServer(conversationId: String): List<Pair<String, String>> {
        return try {
            val response = supabaseClient.from("conversation_participant")
                .select(io.github.jan.supabase.postgrest.query.Columns.raw("user_id, profile(display_name, avatar_url)")) {
                    filter { eq("conversation_id", conversationId) }
                }.decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()

            val cacheList = mutableListOf<UserProfileCacheEntity>()
            val pairResult = response.map { row ->
                val userId =
                    row["user_id"]?.toString()?.replace("\"", "")?.lowercase()?.trim() ?: ""
                val profileElement = row["profile"]
                var displayName = "Thành viên"
                var avatarUrl: String? = null

                try {
                    if (profileElement != null) {
                        displayName =
                            profileElement.jsonObject["display_name"]?.jsonPrimitive?.content
                                ?: "Thành viên"
                        avatarUrl = profileElement.jsonObject["avatar_url"]?.jsonPrimitive?.content
                    }
                } catch (e: Exception) {
                }

                cacheList.add(
                    UserProfileCacheEntity(
                        userId = userId,
                        displayName = displayName,
                        avatarUrl = avatarUrl
                    )
                )
                Pair(userId, displayName)
            }

            // Lưu đè thông tin tươi mới nhất xuống Local DB máy để xóa hẳn độ trễ nháy chữ
            chatDAO.insertProfilesToCache(cacheList)
            pairResult
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateGroupName(conversationId: String, newName: String) {
        try {
            supabaseClient.from("conversation").update(mapOf("name" to newName)) {
                filter { eq("id", conversationId) }
            }

            val currentConv = chatDAO.getConversationById(conversationId).firstOrNull()

            val actualWalletId = currentConv?.walletId
            if (!actualWalletId.isNullOrBlank() && actualWalletId != "wallet_default") {
                supabaseClient.from("group_wallet").update(mapOf("name" to newName)) {
                    filter { eq("id", actualWalletId) }
                }
            }

            currentConv?.let {
                chatDAO.insertConversation(it.copy(name = newName))
            }

            sendSystemMessage(conversationId, "Tên nhóm đã được đổi thành '$newName'")
        } catch (e: Exception) {
        }
    }

    suspend fun kickMember(conversationId: String, userIdToKick: String, memberName: String) {
        try {
            // 1. Xóa ở server
            supabaseClient.from("conversation_participant").delete {
                filter {
                    eq("conversation_id", conversationId)
                    eq("user_id", userIdToKick.lowercase())
                }
            }

            // 2. Xóa ở local
            if (userIdToKick.lowercase() == getAuthenticatedUserId()) {
                chatDAO.deleteConversationById(conversationId)
            }

            // 3. Gửi tin hệ thống để các máy khác cập nhật
            sendSystemMessage(conversationId, "Đã mời $memberName rời khỏi nhóm.")
        } catch (e: Exception) { e.printStackTrace() }
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

            if (currentUserId.isBlank()) return false

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
            _walletUpdateSignal.tryEmit(conversationId)
            sendSystemMessage(
                conversationId,
                "Quỹ nhóm '$walletName' đã được thiết lập thành công."
            )
            true
        } catch (e: Exception) {
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

            if (walletId.isBlank() || walletId == "wallet_default") return false

            val actualGroupMembers = fetchGroupMembersFromServer(conversationId)
            val currentUserName =
                actualGroupMembers.find { it.first == currentUserId }?.second ?: "Thành viên"

            val rpcArgs = WalletRpcArgs(
                walletId = walletId,
                amount = amount,
                note = note
            )

            val messageText = if (txType == "deposit") {
                "Hệ thống: ${currentUserName} đã nộp ${
                    String.format(
                        "%,.0f",
                        amount
                    )
                } VND vào quỹ nhóm. Nội dung: $note"
            } else {
                "Hệ thống: ${currentUserName} đã rút ${
                    String.format(
                        "%,.0f",
                        amount
                    )
                } VND từ quỹ nhóm. Nội dung: $note"
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
        } catch (e: Exception) {
        }
    }

    private suspend fun performNetworkSend(message: Message) {
        try {
            var finalImageUrl = message.imageUrl
            val currentUserId = getAuthenticatedUserId()

            if (message.imageUrl != null && (message.imageUrl.startsWith("content://") || message.imageUrl.startsWith(
                    "file://"
                ))
            ) {
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
                    }
                } catch (storageErr: Exception) {
                }
            }

            val finalSenderId = currentUserId

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
            println("DEBUG: Đang gửi tin vào conv: ${message.conversationId}")

            supabaseClient.from("message").insert(messageDto)

            chatDAO.updateMessage(message.copy(syncStatus = MessageSyncStatus.SENT))
        } catch (e: Exception) {
            println("LỖI GỬI TIN CHI TIẾT: ${e.localizedMessage}")
            e.printStackTrace()
            chatDAO.updateMessage(message.copy(syncStatus = MessageSyncStatus.FAILED))
        }
    }

    suspend fun retryMessage(message: Message) {
        val reloadingMessage = message.copy(syncStatus = MessageSyncStatus.PENDING)
        chatDAO.updateMessage(reloadingMessage)
        performNetworkSend(reloadingMessage)
    }

    // ── WALLET HELPERS ────────────────────────────────────────────────────────

    /**
     * Returns all wallets the current user is a member of, with their role.
     * Used by DiaryViewModel to populate PaymentSourceSelector.
     */
    suspend fun getUserWalletsWithRoles(): List<WalletWithRole> {
        return try {
            val currentUserId = getAuthenticatedUserId()
            if (currentUserId.isBlank()) return emptyList()

            val memberships = supabaseClient.from("wallet_membership")
                .select {
                    filter { eq("user_id", currentUserId) }
                }.decodeList<SupabaseWalletMembershipDto>()

            memberships.mapNotNull { membership ->
                runCatching {
                    val wallet = supabaseClient.from("group_wallet")
                        .select { filter { eq("id", membership.walletId) } }
                        .decodeSingle<SupabaseGroupWalletDto>()
                    WalletWithRole(
                        walletId   = wallet.id,
                        walletName = wallet.name,
                        role       = membership.role
                    )
                }.getOrNull()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Fetch current balance for a wallet directly by walletId.
     * Cheaper than going through conversation for the pre-check path.
     */
    suspend fun getWalletBalanceById(walletId: String): Double {
        return try {
            val wallet = supabaseClient.from("group_wallet")
                .select { filter { eq("id", walletId) } }
                .decodeSingle<Map<String, kotlinx.serialization.json.JsonElement>>()
            wallet["balance"]?.toString()?.replace("\"", "")?.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Called from DiaryViewModel after the item is saved locally.
     * 1. Calls rpc_wallet_purchase (atomic balance deduct + transaction row).
     * 2. Sends a system message with item image into the group chat.
     *
     * Returns Result.success on success, Result.failure with a descriptive
     * exception when balance is insufficient or RPC fails.
     */
    suspend fun executePurchaseTransaction(
        walletId:       String,
        amount:         Double,
        itemId:         String?,
        note:           String,
        imageUrl:       String? = null,
        conversationId: String? = null
    ): Result<Unit> = runCatching {
        val currentUserId = getAuthenticatedUserId()

        val rpcArgs = PurchaseRpcArgs(
            walletId = walletId,
            amount   = amount,
            note     = note,
            itemId   = null
        )
        // rpc_wallet_purchase raises a Postgres exception when balance is insufficient;
        // the runCatching wrapper converts that into Result.failure automatically.
        supabaseClient.postgrest.rpc(
            function   = "rpc_wallet_purchase",
            parameters = rpcArgs
        )

        // Phân giải conversationId từ wallet_id khi không được cung cấp
        val convId = conversationId ?: runCatching {
            supabaseClient.from("conversation")
                .select { filter { eq("wallet_id", walletId) } }
                .decodeSingle<SupabaseConversationDto>()
                .id
        }.getOrNull()

        if (convId != null) {
            val members = fetchGroupMembersFromServer(convId)
            val actorName = members.find { it.first == currentUserId }?.second ?: "Thành viên"
            val amountFormatted = String.format("%,.0f", amount)
            val body = "$actorName đã mua \"$note\" từ quỹ nhóm — $amountFormatted VND"

            sendMessage(
                conversationId = convId,
                senderId       = "system",
                body           = body,
                imageUrl       = imageUrl,  // item image shown in the chat bubble
                isSystem       = true
            )
        }
    }
}