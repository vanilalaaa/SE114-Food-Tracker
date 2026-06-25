package com.SE114.food_tracker.data.repository

import android.content.Context
import android.net.Uri
import com.SE114.food_tracker.data.local.dao.ChatDAO
import com.SE114.food_tracker.data.local.dao.MessageWithProfile
import com.SE114.food_tracker.data.local.entities.ConversationParticipant
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
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
import io.github.jan.supabase.realtime.broadcast
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import timber.log.Timber

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
        @SerialName("wallet_id") val walletId: String? = null,
        @SerialName("last_message_at") val lastMessageAt: Long = 0L,
        @SerialName("last_message_snippet") val lastMessageSnippet: String? = null
    )

    @Serializable
    data class PeerProfileDto(
        @SerialName("id") val id: String,
        @SerialName("display_name") val displayName: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null
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
        @SerialName("p_wallet_id") val walletId: String,
        @SerialName("p_amount") val amount: Double,
        @SerialName("p_note") val note: String
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

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** A subscribed realtime channel plus the scope its collectors run on, so one
     *  conversation can be torn down without affecting any other. */
    private class ChannelHandle(val channel: RealtimeChannel, val scope: CoroutineScope)

    private val activeChannels = ConcurrentHashMap<String, ChannelHandle>()
    private val reconnectingChannels = ConcurrentHashMap.newKeySet<String>()
    private val reconnectBackoff = ConcurrentHashMap<String, Long>()
    private val globalChannelStarted = AtomicBoolean(false)
    private val _memberUpdateSignal = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val subscribeDeferreds = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    val memberUpdateSignal = _memberUpdateSignal.asSharedFlow()
    private val _walletUpdateSignal = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val walletUpdateSignal = _walletUpdateSignal.asSharedFlow()

    // Broadcast event name — must be identical on sender and all receivers.
    private val BROADCAST_EVENT_NEW_MESSAGE = "new_message"

    private fun parseServerTimeToLong(serverTimeStr: String?): Long {
        if (serverTimeStr.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching { Instant.parse(serverTimeStr).toEpochMilliseconds() }
            // Postgres may render timestamptz space-separated ("2026-06-26 12:00:00+00")
            // rather than ISO-8601 with a 'T'; normalise once before giving up.
            .recoverCatching { Instant.parse(serverTimeStr.replace(' ', 'T')).toEpochMilliseconds() }
            .getOrDefault(System.currentTimeMillis())
    }

    fun getAuthenticatedUserId(): String {
        return supabaseClient.auth.currentUserOrNull()?.id?.lowercase() ?: ""
    }

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

            val convIds = supabaseClient.from("conversation_participant")
                .select { filter { eq("user_id", currentUserId) } }
                .decodeList<SupabaseParticipantDto>()
                .map { it.conversationId }
                .distinct()
            if (convIds.isEmpty()) return

            // One round-trip for every conversation the user belongs to, replacing the
            // old per-row select loop (N+1). last_message_at/snippet come from the server
            // (maintained by trigger) so the list keeps its newest-first order after a fetch.
            val conversations = supabaseClient.from("conversation")
                .select { filter { isIn("id", convIds) } }
                .decodeList<SupabaseConversationDto>()

            chatDAO.insertConversations(
                conversations.map { dto ->
                    LocalConversation(
                        id                 = dto.id,
                        name               = dto.name ?: "Trò chuyện 1-1",
                        isGroup            = dto.isGroup,
                        walletId           = dto.walletId ?: "wallet_default",
                        lastMessageAt      = dto.lastMessageAt,
                        lastMessageSnippet = dto.lastMessageSnippet
                    )
                }
            )

            // Persist the participant graph + peer profiles in two batched queries so the
            // list can render the 1-1 peer's name/avatar without a per-conversation round-trip.
            // The current user's own row is left to markAsRead so we never clobber last_read_at.
            val peers = supabaseClient.from("conversation_participant")
                .select { filter { isIn("conversation_id", convIds) } }
                .decodeList<SupabaseParticipantDto>()
                .filter { it.userId.lowercase() != currentUserId }

            chatDAO.insertParticipants(
                peers.map {
                    ConversationParticipant(
                        conversationId = it.conversationId,
                        userId         = it.userId.lowercase(),
                        isAdmin        = it.isAdmin
                    )
                }
            )

            val peerIds = peers.map { it.userId.lowercase() }.distinct()
            if (peerIds.isNotEmpty()) {
                val profiles = supabaseClient.from("profile")
                    .select(
                        io.github.jan.supabase.postgrest.query.Columns.raw("id, display_name, avatar_url")
                    ) { filter { isIn("id", peerIds) } }
                    .decodeList<PeerProfileDto>()

                chatDAO.insertProfilesToCache(
                    profiles.map {
                        UserProfileCacheEntity(
                            userId      = it.id.lowercase(),
                            displayName = it.displayName ?: "Thành viên",
                            avatarUrl   = it.avatarUrl
                        )
                    }
                )
            }
        } catch (e: Exception) {
            Timber.tag("Chat").e(e, "fetchAndSaveConversationsToLocal failed")
        }
    }

    // ── CHỨC NĂNG REALTIME CHANNEL ──

    // Hàm chờ channel ready
    private suspend fun awaitChannelReady(conversationId: String, timeoutMs: Long = 2000): Boolean {
        val deferred = subscribeDeferreds[conversationId]
        return if (deferred != null) {
            try {
                withTimeout(timeoutMs) { deferred.await() }
                true
            } catch (e: TimeoutCancellationException) {
                println("Timeout đợi channel subscribe cho $conversationId")
                false
            }
        } else {
            // Nếu chưa có deferred, kiểm tra xem channel đã có trong activeChannels chưa
            activeChannels.containsKey(conversationId)
        }
    }
    fun subscribeToChatRealtime(conversationId: String) {
        repositoryScope.launch {
            if (activeChannels.containsKey(conversationId)) {
                subscribeDeferreds[conversationId]?.complete(Unit)
                return@launch
            }

            val deferred = CompletableDeferred<Unit>()
            subscribeDeferreds[conversationId] = deferred
            try {
                // Collectors run on a child scope tied to this channel so leaving the chat
                // (unsubscribeFromChatRealtime) cancels exactly these collectors, nothing else.
                val channelScope = CoroutineScope(
                    SupervisorJob(repositoryScope.coroutineContext[Job]) + Dispatchers.IO
                )
                val channel = supabaseClient.channel("chat_channel_$conversationId")

                // ── Broadcast flow (PRIMARY path — ~50–200 ms) ────────────────────────────
                // The sender calls channel.broadcast() immediately after the REST insert
                // succeeds, so other members receive the message without waiting for WAL.
                val broadcastMessageFlow = channel.broadcastFlow<SupabaseMessageDto>(
                    event = BROADCAST_EVENT_NEW_MESSAGE
                )

                // ── Postgres CDC flow (FALLBACK path — ~2–5 s) ───────────────────────────
                // Still needed to catch:
                //   • Messages sent from web / Supabase Studio that never broadcast.
                //   • The sender's own message coming back (deduped by serverId check).
                //   • Any broadcast that was dropped due to a transient WebSocket blip.
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
                    channel.postgresChangeFlow<PostgresAction.Insert>(
                        schema = "public"
                    ) {
                        table = "conversation_participant"
                    }
                val participantDelete =
                    channel.postgresChangeFlow<PostgresAction.Delete>(
                        schema = "public"
                    ) {
                        table = "conversation_participant"
                    }

                // Collectors MUST be launched before channel.subscribe() so the
                // supabase-kt library registers the filter/event during the subscribe
                // handshake. Launching after subscribe() causes flows to never emit.

                // ── BROADCAST collector — instant delivery ─────────────────────────────
                channelScope.launch {
                    broadcastMessageFlow.collect { dto ->
                        // Tránh trùng lặp trên chính máy người gửi: nếu id người gửi trùng với mình thì bỏ qua
                        if (dto.senderId == getAuthenticatedUserId()) return@collect

                        if (dto.conversationId == conversationId) {
                            val exist = chatDAO.getMessageByServerId(dto.id ?: "")
                            if (exist == null) {
                                val incomingMessage = Message(
                                    localId        = dto.id ?: UUID.randomUUID().toString(),
                                    serverId       = dto.id,
                                    conversationId = dto.conversationId,
                                    senderId       = dto.senderId.lowercase(),
                                    body           = dto.body,
                                    imageUrl       = dto.imageUrl,
                                    isSystem       = dto.isSystem,
                                    syncStatus     = MessageSyncStatus.SENT,
                                    createdAt      = parseServerTimeToLong(dto.createdAt)
                                )
                                chatDAO.insertMessage(incomingMessage)
                                chatDAO.updateLastMessage(
                                    conversationId = incomingMessage.conversationId,
                                    messageAt      = incomingMessage.createdAt,
                                    snippet        = if (incomingMessage.isSystem) "📢 Tin nhắn hệ thống" else incomingMessage.body
                                )
                            }
                        }
                    }
                }

                // ── CDC fallback collector — deduplication via serverId ─────────────────
                channelScope.launch {
                    changeFlow.collect { action ->
                        val dto = action.decodeRecord<SupabaseMessageDto>()
                        // Cũng chặn trùng lặp cho chính người gửi tại luồng CDC luôn
                        if (dto.senderId == getAuthenticatedUserId()) return@collect

                        if (dto.conversationId == conversationId) {
                            val exist = chatDAO.getMessageByServerId(dto.id ?: "")
                            if (exist == null) {
                                val incomingMessage = Message(
                                    localId        = dto.id ?: UUID.randomUUID().toString(),
                                    serverId       = dto.id,
                                    conversationId = dto.conversationId,
                                    senderId       = dto.senderId.lowercase(),
                                    body           = dto.body,
                                    imageUrl       = dto.imageUrl,
                                    isSystem       = dto.isSystem,
                                    syncStatus     = MessageSyncStatus.SENT,
                                    createdAt      = parseServerTimeToLong(dto.createdAt)
                                )
                                chatDAO.insertMessage(incomingMessage)
                                chatDAO.updateLastMessage(
                                    conversationId = incomingMessage.conversationId,
                                    messageAt      = incomingMessage.createdAt,
                                    snippet        = if (incomingMessage.isSystem) "📢 Tin nhắn hệ thống" else incomingMessage.body
                                )
                            }
                        }
                    }
                }

                // Bắn tín hiệu nạp lại thành viên khi có người vào
                channelScope.launch {
                    participantInsert.collect {
                        _memberUpdateSignal.tryEmit(conversationId)
                    }
                }

                // Bắn tín hiệu nạp lại thành viên khi có người ra
                channelScope.launch {
                    participantDelete.collect {
                        _memberUpdateSignal.tryEmit(conversationId)
                    }
                }

                channelScope.launch {
                    conversationUpdateFlow.collect { action ->
                        val updatedDto = action.decodeRecord<SupabaseConversationDto>()
                        if (updatedDto.id == conversationId) {
                            val currentConv =
                                chatDAO.getConversationById(conversationId).firstOrNull()
                            currentConv?.let {
                                chatDAO.insertConversation(
                                    it.copy(
                                        name     = updatedDto.name ?: it.name,
                                        walletId = updatedDto.walletId ?: it.walletId
                                    )
                                )
                            }
                        }
                    }
                }

                channelScope.launch {
                    walletUpdateFlow.collect {
                        _walletUpdateSignal.tryEmit(conversationId)
                    }
                }

                channelScope.launch {
                    transactionInsertFlow.collect {
                        _walletUpdateSignal.tryEmit(conversationId)
                    }
                }

                channel.subscribe()
                activeChannels[conversationId] = ChannelHandle(channel, channelScope)
                reconnectBackoff.remove(conversationId)
                deferred.complete(Unit)
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
                Timber.tag("Chat").e(e, "subscribe failed for $conversationId")
                handleRealtimeReconnect(conversationId)
            }
        }
    }

    /** Cancels this conversation's collectors and unsubscribes its channel. Safe to call
     *  when leaving ChatScreen; a no-op if the conversation was never subscribed. */
    fun unsubscribeFromChatRealtime(conversationId: String) {
        reconnectingChannels.remove(conversationId)
        reconnectBackoff.remove(conversationId)
        subscribeDeferreds.remove(conversationId)
        val handle = activeChannels.remove(conversationId) ?: return
        handle.scope.cancel()
        repositoryScope.launch { runCatching { handle.channel.unsubscribe() } }
    }

    fun subscribeToGlobalConversationsRealtime() {
        val currentUserId = getAuthenticatedUserId()
        if (currentUserId.isBlank()) return
        // The repo is a singleton but every ChatViewModel calls this; keep exactly one
        // global channel so message inserts aren't processed by several collectors.
        if (!globalChannelStarted.compareAndSet(false, true)) return

        repositoryScope.launch {
            try {
                val globalChannel = supabaseClient.channel("global_conv_channel_$currentUserId")

                val participantInsertFlow =
                    globalChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                        table = "conversation_participant"
                    }
                val participantDeleteFlow =
                    globalChannel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
                        table = "conversation_participant"
                    }
                val participantUpdateFlow =
                    globalChannel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                        table = "conversation_participant"
                    }
                // Listening to every message insert (RLS scopes it to the user's conversations)
                // and writing it to Room is what lets the list — a Room Flow — react to new
                // messages in ANY conversation, reordering newest-first and recomputing unread,
                // not just the conversation currently open.
                val messageInsertFlow =
                    globalChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                        table = "message"
                    }

                repositoryScope.launch {
                    participantInsertFlow.collect { action ->
                        val userIdStr =
                            action.record["user_id"]?.toString()?.replace("\"", "")?.lowercase()
                        if (userIdStr == currentUserId) fetchAndSaveConversationsToLocal()
                    }
                }

                repositoryScope.launch {
                    participantDeleteFlow.collect { action ->
                        val userIdStr = action.oldRecord["user_id"]?.toString()?.replace("\"", "")?.lowercase()
                        val convIdStr = action.oldRecord["conversation_id"]?.toString()?.replace("\"", "")
                        if (userIdStr == currentUserId) {
                            if (convIdStr != null) chatDAO.deleteConversationById(convIdStr)
                            fetchAndSaveConversationsToLocal()
                        }
                    }
                }

                // Read elsewhere: another device advanced last_read_at — mirror it locally so
                // the list de-emphasises a conversation the user already read.
                repositoryScope.launch {
                    participantUpdateFlow.collect { action ->
                        val rec = action.record
                        val userIdStr = rec["user_id"]?.toString()?.replace("\"", "")?.lowercase()
                        if (userIdStr == currentUserId) {
                            val convIdStr = rec["conversation_id"]?.toString()?.replace("\"", "")
                            val lastRead = rec["last_read_at"]?.toString()?.replace("\"", "")?.toLongOrNull()
                            if (convIdStr != null && lastRead != null) {
                                chatDAO.markConversationRead(convIdStr, currentUserId, lastRead)
                            }
                        }
                    }
                }

                repositoryScope.launch {
                    messageInsertFlow.collect { action ->
                        val dto = action.decodeRecord<SupabaseMessageDto>()
                        val exist = chatDAO.getMessageByServerId(dto.id ?: "")
                        if (exist == null) {
                            val incoming = Message(
                                localId        = dto.id ?: UUID.randomUUID().toString(),
                                serverId       = dto.id,
                                conversationId = dto.conversationId,
                                senderId       = dto.senderId.lowercase(),
                                body           = dto.body,
                                imageUrl       = dto.imageUrl,
                                isSystem       = dto.isSystem,
                                syncStatus     = MessageSyncStatus.SENT,
                                createdAt      = parseServerTimeToLong(dto.createdAt)
                            )
                            chatDAO.insertMessage(incoming)
                            chatDAO.updateLastMessage(
                                conversationId = incoming.conversationId,
                                messageAt      = incoming.createdAt,
                                snippet        = if (incoming.isSystem) "📢 Tin nhắn hệ thống" else incoming.body
                            )
                        }
                    }
                }

                // subscribe() after collectors — same rule as the chat channel above.
                globalChannel.subscribe()
            } catch (e: Exception) {
                globalChannelStarted.set(false)
                Timber.tag("Chat").e(e, "global channel subscribe failed")
            }
        }
    }

    private fun handleRealtimeReconnect(conversationId: String) {
        // newKeySet().add returns false when already present — one reconnect at a time.
        if (!reconnectingChannels.add(conversationId)) return
        repositoryScope.launch {
            val backoff = reconnectBackoff.getOrDefault(conversationId, 2000L)
            try {
                delay(backoff)
                // Drop any stale channel + collectors before re-subscribing so activeChannels
                // never holds two channels for the same conversation.
                activeChannels.remove(conversationId)?.let { stale ->
                    stale.scope.cancel()
                    runCatching { stale.channel.unsubscribe() }
                }
                subscribeDeferreds.remove(conversationId)
                reconnectBackoff[conversationId] = (backoff * 2).coerceAtMost(60000L)
            } finally {
                reconnectingChannels.remove(conversationId)
            }
            // Re-subscribe from scratch; a fresh subscribe resets the backoff on success.
            subscribeToChatRealtime(conversationId)
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
            localId        = localId,
            serverId       = null,
            conversationId = conversationId,
            senderId       = senderId.lowercase(),
            body           = body,
            imageUrl       = imageUrl,
            isSystem       = isSystem,
            syncStatus     = MessageSyncStatus.PENDING,
            createdAt      = System.currentTimeMillis()
        )
        chatDAO.insertMessage(pendingMessage)
        chatDAO.updateLastMessage(
            conversationId = conversationId,
            messageAt      = pendingMessage.createdAt,
            snippet        = if (isSystem) "📢 Tin nhắn hệ thống" else body
        )
        performNetworkSend(pendingMessage)
    }

    suspend fun getOrCreateOneToOneChat(friendUserId: String): String? {
        return try {
            val currentUserId = getAuthenticatedUserId()

            // Callers must pass the friend's UUID, not a nickname.
            if (friendUserId.length < 36) {
                Timber.tag("Chat").e("getOrCreateOneToOneChat needs a UUID, got '$friendUserId'")
                return null
            }

            val targetFriendId = friendUserId.lowercase()

            // Single server-side lookup for an existing direct conversation between the
            // two users, replacing the per-participation scan (2 queries per row).
            val existingId = runCatching {
                supabaseClient.postgrest.rpc(
                    function   = "find_direct_conversation",
                    parameters = buildJsonObject { put("p_friend", targetFriendId) }
                ).decodeAsOrNull<String>()
            }.getOrNull()
            if (!existingId.isNullOrBlank()) return existingId

            val newChatUuid = UUID.randomUUID().toString()
            supabaseClient.from("conversation").insert(mapOf(
                "id"       to newChatUuid,
                "is_group" to false,
                "name"     to null
            ))
            supabaseClient.from("conversation_participant").insert(listOf(
                SupabaseParticipantDto(newChatUuid, currentUserId, false),
                SupabaseParticipantDto(newChatUuid, targetFriendId, false)
            ))
            newChatUuid
        } catch (e: Exception) {
            Timber.tag("Chat").e(e, "getOrCreateOneToOneChat failed")
            null
        }
    }

    suspend fun createGroupChat(groupName: String, memberUserIds: List<String>): String? {
        return try {
            val groupUuid = UUID.randomUUID().toString()

            val newConversation = SupabaseConversationDto(
                id       = groupUuid,
                name     = groupName,
                isGroup  = true,
                walletId = null
            )

            supabaseClient.from("conversation").insert(newConversation)

            val currentUserId = getAuthenticatedUserId()
            val allMembers = (memberUserIds + currentUserId).map { it.lowercase() }.distinct()

            val participantRows = allMembers.map { userId ->
                SupabaseParticipantDto(
                    conversationId = groupUuid,
                    userId         = userId,
                    isAdmin        = userId == currentUserId
                )
            }
            supabaseClient.from("conversation_participant").insert(participantRows)

            val localGroup = LocalConversation(
                id       = groupUuid,
                name     = groupName,
                isGroup  = true,
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
                        userId      = userId,
                        displayName = displayName,
                        avatarUrl   = avatarUrl
                    )
                )
                Pair(userId, displayName)
            }

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
            supabaseClient.from("conversation_participant").delete {
                filter {
                    eq("conversation_id", conversationId)
                    eq("user_id", userIdToKick.lowercase())
                }
            }

            if (userIdToKick.lowercase() == getAuthenticatedUserId()) {
                chatDAO.deleteConversationById(conversationId)
            }

            sendSystemMessage(conversationId, "Đã mời $memberName rời khỏi nhóm.")
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun sendSystemMessage(conversationId: String, content: String) {
        sendMessage(
            conversationId = conversationId,
            senderId       = getAuthenticatedUserId(),
            body           = content,
            imageUrl       = null,
            isSystem       = true
        )
    }

    suspend fun createGroupWalletForExistingChat(
        conversationId: String,
        memberUserIds: List<String>
    ): Boolean {
        return try {
            val walletUuid    = java.util.UUID.randomUUID().toString()
            val currentUserId = getAuthenticatedUserId()

            if (currentUserId.isBlank()) return false

            val currentConv = chatDAO.getConversationById(conversationId).first()
            val walletName  = currentConv?.name ?: "Quỹ nhóm"

            val walletDto = SupabaseGroupWalletDto(
                id        = walletUuid,
                name      = walletName,
                balance   = 0.0,
                createdBy = currentUserId
            )
            supabaseClient.from("group_wallet").insert(walletDto)

            val allMembers = (memberUserIds + currentUserId).map { it.lowercase() }.distinct()
            val membershipRows = allMembers.map { userId ->
                SupabaseWalletMembershipDto(
                    walletId = walletUuid,
                    userId   = userId,
                    role     = if (userId == currentUserId) "owner" else "member"
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
            val currentUserId    = getAuthenticatedUserId()
            val conversation     = chatDAO.getConversationById(conversationId).first()
            val walletId         = conversation?.walletId ?: ""

            if (walletId.isBlank() || walletId == "wallet_default") return false

            val actualGroupMembers = fetchGroupMembersFromServer(conversationId)
            val currentUserName    =
                actualGroupMembers.find { it.first == currentUserId }?.second ?: "Thành viên"

            val rpcArgs = WalletRpcArgs(
                walletId = walletId,
                amount   = amount,
                note     = note
            )

            val messageText = if (txType == "deposit") {
                "Hệ thống: ${currentUserName} đã nộp ${String.format("%,.0f", amount)} VND vào quỹ nhóm. Nội dung: $note"
            } else {
                "Hệ thống: ${currentUserName} đã rút ${String.format("%,.0f", amount)} VND từ quỹ nhóm. Nội dung: $note"
            }

            when (txType) {
                "deposit" -> {
                    supabaseClient.postgrest.rpc(
                        function   = "rpc_wallet_deposit",
                        parameters = rpcArgs
                    )
                    sendMessage(
                        conversationId = conversationId,
                        senderId       = "system",
                        body           = messageText,
                        imageUrl       = null,
                        isSystem       = true
                    )
                }
                "withdrawal" -> {
                    supabaseClient.postgrest.rpc(
                        function   = "rpc_wallet_withdraw",
                        parameters = rpcArgs
                    )
                    sendMessage(
                        conversationId = conversationId,
                        senderId       = "system",
                        body           = messageText,
                        imageUrl       = null,
                        isSystem       = true
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
                        serverId   = dto.id,
                        body       = dto.body,
                        imageUrl   = dto.imageUrl ?: existingLocalMessage.imageUrl,
                        syncStatus = MessageSyncStatus.SENT,
                        createdAt  = parseServerTimeToLong(dto.createdAt)
                    )
                    chatDAO.updateMessage(updatedMessage)
                } else {
                    val newLocalMessage = Message(
                        localId        = dto.id ?: UUID.randomUUID().toString(),
                        serverId       = dto.id,
                        conversationId = dto.conversationId,
                        senderId       = dto.senderId.lowercase(),
                        body           = dto.body,
                        imageUrl       = dto.imageUrl,
                        isSystem       = dto.isSystem,
                        syncStatus     = MessageSyncStatus.SENT,
                        createdAt      = parseServerTimeToLong(dto.createdAt)
                    )
                    chatDAO.insertMessage(newLocalMessage)
                    chatDAO.updateLastMessage(
                        conversationId = newLocalMessage.conversationId,
                        messageAt      = newLocalMessage.createdAt,
                        snippet        = newLocalMessage.body
                    )
                }
            }
        } catch (e: Exception) {
        }
    }

    private suspend fun performNetworkSend(message: Message) {
        try {
            var finalImageUrl = message.imageUrl
            val currentUserId = getAuthenticatedUserId()

            // Xử lý upload ảnh (giữ nguyên, nhưng không block broadcast)
            if (message.imageUrl != null && (message.imageUrl.startsWith("content://") || message.imageUrl.startsWith("file://"))) {
                try {
                    val uri = android.net.Uri.parse(message.imageUrl)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val fileBytes = inputStream?.use { it.readBytes() }

                    if (fileBytes != null) {
                        val storageBucket = supabaseClient.storage.from("chat-images")
                        val fileName = "${UUID.randomUUID()}.jpg"
                        storageBucket.upload(path = fileName, data = fileBytes) { upsert = true }
                        finalImageUrl = storageBucket.publicUrl(fileName)
                    }
                } catch (storageErr: Exception) {
                    storageErr.printStackTrace()
                }
            }

            val finalSenderId = currentUserId

            // 🔥 ĐỒNG BỘ KHÓA CHÍNH: Dùng luôn message.localId làm 'id' gửi đi
            val broadcastPayload = SupabaseMessageDto(
                id             = message.localId, // <--- Bắt buộc truyền id cục bộ lên
                conversationId = message.conversationId,
                senderId       = finalSenderId,
                body           = message.body,
                imageUrl       = finalImageUrl,
                isSystem       = message.isSystem,
                createdAt      = Clock.System.now().toString()
            )

            val channelReady = awaitChannelReady(message.conversationId, 2000)
            if (channelReady) {
                val channel = activeChannels[message.conversationId]?.channel
                if (channel != null) {
                    runCatching {
                        channel.broadcast(event = BROADCAST_EVENT_NEW_MESSAGE, message = broadcastPayload)
                        println("Broadcast đã gửi (siêu tốc) cho tin nhắn ${message.localId}")
                    }.onFailure { broadcastErr ->
                        println("Broadcast thất bại: ${broadcastErr.localizedMessage}")
                    }
                } else {
                    println("Channel null dù ready, fallback CDC")
                }
            } else {
                println("Channel chưa sẵn sàng, fallback CDC")
            }

            // 🔥 BƯỚC 2: TIẾN HÀNH LƯU DỮ LIỆU XUỐNG DATABASE QUA API REST (Chạy sau/song song để bền vững dữ liệu)
            val response    = supabaseClient.from("message").insert(listOf(broadcastPayload)) { select() }
            val insertedDto = response.decodeSingle<SupabaseMessageDto>()

            val successMessage = message.copy(
                syncStatus = MessageSyncStatus.SENT,
                serverId   = insertedDto.id, // Sẽ trùng khớp hoàn toàn với message.localId
                imageUrl   = finalImageUrl,
                senderId   = finalSenderId,
                createdAt  = parseServerTimeToLong(insertedDto.createdAt)
            )

            chatDAO.updateMessage(successMessage)
            println("DEBUG: Tin nhắn đã được lưu kiên cố vào Server Database")

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

    suspend fun markAsRead(conversationId: String) {
        val currentUserId = getAuthenticatedUserId()
        if (currentUserId.isBlank()) return

        val now = System.currentTimeMillis()

        chatDAO.insertParticipantIfAbsent(
            ConversationParticipant(
                conversationId = conversationId,
                userId         = currentUserId,
                lastReadAt     = now
            )
        )
        chatDAO.markConversationRead(conversationId, currentUserId, now)

        // Server marker advances via a gated RPC (security definer) so clients never need
        // direct UPDATE access to conversation_participant.
        runCatching {
            supabaseClient.postgrest.rpc(
                function   = "mark_conversation_read",
                parameters = buildJsonObject { put("p_conversation", conversationId) }
            )
        }
    }

    // ── WALLET HELPERS ────────────────────────────────────────────────────────

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
        supabaseClient.postgrest.rpc(
            function   = "rpc_wallet_purchase",
            parameters = rpcArgs
        )

        val convId = conversationId ?: runCatching {
            supabaseClient.from("conversation")
                .select { filter { eq("wallet_id", walletId) } }
                .decodeSingle<SupabaseConversationDto>()
                .id
        }.getOrNull()

        if (convId != null) {
            val members       = fetchGroupMembersFromServer(convId)
            val actorName     = members.find { it.first == currentUserId }?.second ?: "Thành viên"
            val amountFormatted = String.format("%,.0f", amount)
            val body          = "$actorName đã mua \"$note\" từ quỹ nhóm — $amountFormatted VND"

            sendMessage(
                conversationId = convId,
                senderId       = getAuthenticatedUserId(),
                body           = body,
                imageUrl       = imageUrl,
                isSystem       = true
            )
        }
    }
}