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

@Serializable
data class SupabaseMessageDto(
    @SerialName("id") val id: String? = null,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("body") val body: String?,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("is_system") val isSystem: Boolean
)

@Singleton
class ChatRepository @Inject constructor(
    private val chatDAO: ChatDAO,
    private val supabaseClient: SupabaseClient,
    @ApplicationContext private val context: Context
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private var chatChannel: RealtimeChannel? = null

    // LẤY USER ID THẬT ĐÃ ĐĂNG NHẬP QUA SUPABASE AUTH ĐỂ VƯỢT RLS POLICY
    fun getAuthenticatedUserId(): String {
        return supabaseClient.auth.currentUserOrNull()?.id ?: "vy_id"
    }

    // 1. Lấy dòng chảy tin nhắn từ Local Room DB đổ lên UI
    fun getMessagesStream(conversationId: String): Flow<List<Message>> {
        return chatDAO.getMessagesByConversation(conversationId)
    }

    // ── CHỨC NĂNG 1: REALTIME CHANNEL (SUBSCRIBE + RECONNECT) ──

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
                                createdAt = System.currentTimeMillis()
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
            println("Supabase Realtime: Đang thử kết nối lại phòng $conversationId...")
            try {
                chatChannel?.subscribe()
                isConnected = true
                println("Supabase Realtime: Reconnect thành công!")
            } catch (e: Exception) {
                retryDelay = (retryDelay * 2).coerceAtMost(60000L)
            }
        }
    }

    // ── CHỨC NĂNG 2: GỬI TIN NHẮN (MỒI PENDING) ───

    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        body: String?,
        imageUrl: String?
    ) {
        val localId = UUID.randomUUID().toString()

        val pendingMessage = Message(
            localId = localId,
            serverId = null,
            conversationId = conversationId,
            senderId = senderId,
            body = body,
            imageUrl = imageUrl,
            isSystem = false,
            syncStatus = MessageSyncStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        chatDAO.insertMessage(pendingMessage)
        performNetworkSend(pendingMessage)
    }

    // ── CHỨC NĂNG 3: MẠNG LƯỚI GỬI LÊN SERVER + UPLOAD ẢNH + RETRY  ──

    private suspend fun performNetworkSend(message: Message) {
        try {
            var finalImageUrl = message.imageUrl

            // XỬ LÝ UPLOAD ẢNH THẬT LÊN STORAGE BUCKET
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
                imageUrl = finalImageUrl
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