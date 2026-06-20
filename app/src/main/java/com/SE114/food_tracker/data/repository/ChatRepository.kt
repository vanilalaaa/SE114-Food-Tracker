package com.SE114.food_tracker.data.repository

import android.content.Context
import android.net.Uri
import com.SE114.food_tracker.data.local.dao.ChatDAO
import com.SE114.food_tracker.data.local.entities.Message
import com.SE114.food_tracker.data.local.entities.MessageSyncStatus
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

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private var chatChannel: RealtimeChannel? = null

    // Hàm phụ trợ giúp chuyển đổi chuỗi thời gian Server sang Long để nạp vào Room
    private fun parseServerTimeToLong(serverTimeStr: String?): Long {
        if (serverTimeStr.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", java.util.Locale.US)
            val date = sdf.parse(serverTimeStr)
            date?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                val sdfBackup = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
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

    // ── CHỨC NĂNG 1: REALTIME CHANNEL (ĐÃ ĐỒNG BỘ GIỜ SERVER THẬT) ──

    fun subscribeToChatRealtime(conversationId: String) {
        repositoryScope.launch {
            try {
                chatChannel = supabaseClient.channel("chat_channel_$conversationId")

                val changeFlow = chatChannel?.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "message"
                }

                repositoryScope.launch {
                    changeFlow?.collect { action ->
                        val dto = action.decodeRecord<SupabaseMessageDto>()
                        val currentUserId = getAuthenticatedUserId()

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

                chatChannel?.subscribe()
                println("Supabase Realtime: Subscribe thành công phòng $conversationId")
            } catch (e: Exception) {
                println("Supabase Realtime Lỗi: ${e.localizedMessage}")
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
                chatChannel?.subscribe()
                isConnected = true
                println("Supabase Realtime: Reconnect thành công!")
            } catch (e: Exception) {
                retryDelay = (retryDelay * 2).coerceAtMost(60000L)
            }
        }
    }

    // ── CHỨC NĂNG 2: GỬI TIN NHẮN ───

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

    // ── CHỨC NĂNG CHAT 1-1 ──

    suspend fun getOrCreateOneToOneChat(friendUserId: String): String? {
        return try {
            val currentUserId = getAuthenticatedUserId()

            // Tìm các phòng chat mà người dùng hiện tại tham gia
            val myParticipations = supabaseClient.from("conversation_participant")
                .select {
                    filter { eq("user_id", currentUserId) }
                }.decodeList<Map<String, String>>()

            var existingConversationId: String? = null

            // Quét xem có phòng 1-1 (is_group = false) nào chứa người bạn đó không
            for (part in myParticipations) {
                val convId = part["conversation_id"] ?: continue

                // Kiểm tra xem phòng này có phải chat nhóm không
                val isGroupCheck = supabaseClient.from("conversation")
                    .select {
                        filter { eq("id", convId) }
                    }.decodeSingle<Map<String, Boolean>>()["is_group"] ?: false

                if (!isGroupCheck) {
                    // Nếu không phải nhóm, check tiếp xem người bạn kia có nằm trong phòng này không
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

            // Nếu tìm thấy phòng chat cũ thì trả về ID
            if (existingConversationId != null) {
                return existingConversationId
            }

            // Chưa nhắn bao giờ -> Khởi tạo phòng mới
            val newChatUuid = UUID.randomUUID().toString()
            val conversationMap = mapOf(
                "id" to newChatUuid,
                "is_group" to false,
                "name" to null
            )
            supabaseClient.from("conversation").insert(conversationMap)

            // Thêm cả 2 bạn vào danh sách thành viên (chat 1-1 thì mặc định không cần phân quyền admin)
            val participantRows = listOf(
                mapOf("conversation_id" to newChatUuid, "user_id" to currentUserId, "is_admin" to false),
                mapOf("conversation_id" to newChatUuid, "user_id" to friendUserId, "is_admin" to false)
            )
            supabaseClient.from("conversation_participant").insert(participantRows)

            newChatUuid
        } catch (e: Exception) {
            println("Lỗi luồng xử lý tạo chat 1-1: ${e.localizedMessage}")
            null
        }
    }

    // ── CHỨC NĂNG CHAT NHÓM ──

    suspend fun createGroupChat(groupName: String, memberUserIds: List<String>): String? {
        return try {
            val groupUuid = UUID.randomUUID().toString()
            val conversationMap = mapOf(
                "id" to groupUuid,
                "is_group" to true,
                "name" to groupName,
                "wallet_id" to "wallet_${UUID.randomUUID().toString().take(8)}"
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

            sendSystemMessage(groupUuid, "Hệ thống: Nhóm '$groupName' đã được tạo thành công.")
            groupUuid
        } catch (e: Exception) {
            println("Lỗi tạo nhóm: ${e.localizedMessage}")
            null
        }
    }

    // HÀM KIỂM TRA QUYỀN ADMIN NHANH ĐỂ PHỤC VỤ ẨN/HIỆN NÚT TRÊN UI DIALOG
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

    // ── CHỨC NĂNG 3: MẠNG LƯỚI GỬI LÊN SERVER + CẬP NHẬT THỜI GIAN THẬT ──

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

            val response = supabaseClient.from("message").insert(listOf(messageDto)) {
                select()
            }

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