package com.SE114.food_tracker.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.SE114.food_tracker.core.network.NetworkMonitor
import com.SE114.food_tracker.core.sync.SyncScheduler
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
import kotlinx.coroutines.CoroutineExceptionHandler
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import timber.log.Timber

@Singleton
class ChatRepository @Inject constructor(
    private val chatDAO: ChatDAO,
    private val supabaseClient: SupabaseClient,
    private val networkMonitor: NetworkMonitor,
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
        @SerialName("avatar_url") val avatarUrl: String? = null,
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

    /** Fetch-only view of the current user's participation that also carries the server-side read
     *  marker, so re-login / account-switch can restore last_read_at instead of resetting to 0. */
    @Serializable
    data class MyParticipationDto(
        @SerialName("conversation_id") val conversationId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("is_admin") val isAdmin: Boolean = false,
        @SerialName("last_read_at") val lastReadAt: Long = 0L
    )

    @Serializable
    data class UnreadCountDto(
        @SerialName("conversation_id") val conversationId: String,
        @SerialName("unread_count") val unreadCount: Int = 0
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
        @SerialName("p_amount") val amount: Double,
        @SerialName("p_note") val note: String,
        @SerialName("p_item_id") @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.ALWAYS)
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

    // supabase-kt's CallbackManagerImpl removes a channel's flow callbacks from a non-thread-safe
    // AtomicMutableList during cancellation. Tearing down a channel that has several flows (this
    // app registers 7 per chat channel) races on a stale index and throws IndexOutOfBoundsException
    // on the IO dispatcher — an uncaught crash when leaving a chat / reconnecting / logging out.
    // We can't patch the library, so we contain that teardown exception here instead of crashing.
    private val realtimeErrorHandler = CoroutineExceptionHandler { _, t ->
        Timber.tag("Chat").w(t, "realtime coroutine error contained (channel teardown race)")
    }

    private val repositoryScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO + realtimeErrorHandler)

    /** A subscribed realtime channel plus the scope its collectors run on, so one
     *  conversation can be torn down without affecting any other. */
    private class ChannelHandle(val channel: RealtimeChannel, val scope: CoroutineScope)

    private val activeChannels = ConcurrentHashMap<String, ChannelHandle>()
    private val reconnectingChannels = ConcurrentHashMap.newKeySet<String>()
    private val reconnectBackoff = ConcurrentHashMap<String, Long>()

    // One lock per conversation, serializing its subscribe/unsubscribe/reconnect so they never
    // race on the single channel instance supabase-kt caches per topic (a concurrent op could
    // otherwise unsubscribe the very instance another op just registered).
    private val channelMutexes = ConcurrentHashMap<String, Mutex>()

    // One global channel per authenticated user; rebuilt on an in-process account switch.
    private val globalChannelLock = Any()
    private var globalChannelUserId: String? = null
    private var globalChannelHandle: ChannelHandle? = null
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

    // Server-computed unread count per conversation (messages from others newer than my read
    // marker). Lets the list show an exact number for conversations whose messages aren't synced
    // into Room yet; the ViewModel merges it with the locally-counted unread.
    private val _serverUnreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val serverUnreadCounts: StateFlow<Map<String, Int>> = _serverUnreadCounts.asStateFlow()

    // Broadcast event name — must be identical on sender and all receivers.
    private val BROADCAST_EVENT_NEW_MESSAGE = "new_message"

    private fun parseServerTimeToLong(serverTimeStr: String?): Long {
        if (serverTimeStr.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching { Instant.parse(serverTimeStr).toEpochMilliseconds() }
            // Postgres may render timestamptz space-separated ("2026-06-26 12:00:00+00")
            // rather than ISO-8601 with a 'T'; normalise once before giving up.
            .recoverCatching {
                Instant.parse(serverTimeStr.replace(' ', 'T')).toEpochMilliseconds()
            }
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
            // Tách danh sách gốc để vừa lấy convIds vừa lấy được quyền isAdmin của bạn ──
            val myParticipants = supabaseClient.from("conversation_participant")
                .select { filter { eq("user_id", currentUserId) } }
                .decodeList<MyParticipationDto>()

            val convIds = myParticipants.map { it.conversationId }.distinct()
            // val convIds = supabaseClient.from("conversation_participant")
               // .select { filter { eq("user_id", currentUserId) } }
                //.decodeList<SupabaseParticipantDto>()
                //.map { it.conversationId }
                //.distinct()
            // Tìm và xóa các phòng chat local ma do bị kick hoặc giải tán
            val localConvs = chatDAO.getAllConversations().firstOrNull() ?: emptyList()
            val localIds = localConvs.map { it.id }
            val convIdSet = convIds.toSet()
            val orphanedIds = localIds.filter { it !in convIdSet }
            orphanedIds.forEach { id ->
                deleteConversationLocal(id)
            }
            if (convIds.isEmpty()) return

            // One round-trip for every conversation the user belongs to, replacing the
            // old per-row select loop (N+1). last_message_at/snippet come from the server
            // (maintained by trigger) so the list keeps its newest-first order after a fetch.
            val conversations = supabaseClient.from("conversation")
                .select { filter { isIn("id", convIds) } }
                .decodeList<SupabaseConversationDto>()

            // REPLACE would otherwise reset created_at to "now" (the server DTO has none) and
            // could overwrite a newer locally-tracked last_message_at with a stale snapshot —
            // both scramble the newest-first order. Read existing rows and preserve those fields.
            val existing = chatDAO.getConversationsByIds(convIds).associateBy { it.id }
            val mapped = conversations.map { dto ->
                val prev = existing[dto.id]
                val keepLocalLast = (prev?.lastMessageAt ?: 0L) > dto.lastMessageAt
                LocalConversation(
                    id = dto.id,
                    name = dto.name ?: "Trò chuyện 1-1",
                    isGroup = dto.isGroup,
                    avatarUrl = dto.avatarUrl ?: prev?.avatarUrl,
                    lastMessageAt = if (keepLocalLast) prev?.lastMessageAt
                        ?: 0L else dto.lastMessageAt,
                    lastMessageSnippet = if (keepLocalLast) prev?.lastMessageSnippet else dto.lastMessageSnippet,
                    createdAt = prev?.createdAt ?: System.currentTimeMillis()
                )
            }
            // Only write rows that actually changed: REPLACE-ing every row on each refresh
            // invalidates the Room Flow and recomposes the whole list, which is the pull-to-
            // refresh jank. A no-op sync now writes nothing.
            val changed = mapped.filter { it != existing[it.id] }
            if (changed.isNotEmpty()) chatDAO.insertConversations(changed)
            // ── BỔ SUNG AN TOÀN: Đưa chính mình vào table local để thỏa mãn điều kiện INNER JOIN ──
            // Chiến lược IGNORE sẽ tự động bỏ qua nếu bản ghi đã tồn tại, đảm bảo không clobber dữ liệu đọc tin thực tế.
            // Restore the read marker from the server (the source of truth). On a fresh login /
            // account-switch the local participant row is gone, so insertIfAbsent seeds it with the
            // server value instead of 0 (which made every conversation look unread). When a row
            // already exists, markConversationRead advances it forward-only — so a marker read on
            // another device is mirrored, but a locally-newer (read-offline) marker is never undone.
            myParticipants.forEach { participant ->
                chatDAO.insertParticipantIfAbsent(
                    ConversationParticipant(
                        conversationId = participant.conversationId,
                        userId = currentUserId,
                        isAdmin = participant.isAdmin,
                        lastReadAt = participant.lastReadAt
                    )
                )
                chatDAO.markConversationRead(participant.conversationId, currentUserId, participant.lastReadAt)
            }
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
                        userId = it.userId.lowercase(),
                        isAdmin = it.isAdmin
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
                            userId = it.id.lowercase(),
                            displayName = it.displayName ?: "Thành viên",
                            avatarUrl = it.avatarUrl
                        )
                    }
                )
            }

            refreshServerUnreadCounts()
        } catch (e: Exception) {
            Timber.tag("Chat").e(e, "fetchAndSaveConversationsToLocal failed")
        }
    }

    /** Pull the server's per-conversation unread count for the current user. Best-effort: a
     *  failure leaves the previous counts in place (the local message-based count still works). */
    private suspend fun refreshServerUnreadCounts() {
        runCatching {
            supabaseClient.postgrest.rpc("unread_message_counts").decodeList<UnreadCountDto>()
        }.onSuccess { rows ->
            _serverUnreadCounts.value = rows.associate { it.conversationId to it.unreadCount }
        }.onFailure { Timber.tag("Chat").e(it, "unread_message_counts failed") }
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

    private fun channelMutex(conversationId: String): Mutex =
        channelMutexes.getOrPut(conversationId) { Mutex() }

    fun subscribeToChatRealtime(conversationId: String) {
        repositoryScope.launch {
            // Serialize subscribe/unsubscribe/reconnect per conversation: supabase-kt caches one
            // channel instance per topic, so overlapping ops would corrupt its state (e.g. an
            // abandoned subscribe unsubscribing the instance another op just registered).
            val mutex = channelMutex(conversationId)
            mutex.lock()
            val deferred = CompletableDeferred<Unit>()
            try {
                if (activeChannels.containsKey(conversationId)) {
                    subscribeDeferreds[conversationId]?.complete(Unit)
                    return@launch
                }
                subscribeDeferreds[conversationId] = deferred
                // Collectors run on a child scope tied to this channel so leaving the chat
                // (unsubscribeFromChatRealtime) cancels exactly these collectors, nothing else.
                val channelScope = CoroutineScope(
                    SupervisorJob(repositoryScope.coroutineContext[Job]) + Dispatchers.IO + realtimeErrorHandler
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

//                val walletUpdateFlow =
//                    channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
//                        table = "group_wallet"
//                    }
//
//                val transactionInsertFlow =
//                    channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
//                        table = "wallet_transaction"
//                    }

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
                                chatDAO.updateLastMessage(
                                    conversationId = incomingMessage.conversationId,
                                    messageAt = incomingMessage.createdAt,
                                    snippet = if (incomingMessage.isSystem) "📢 Tin nhắn hệ thống" else incomingMessage.body
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
                                chatDAO.updateLastMessage(
                                    conversationId = incomingMessage.conversationId,
                                    messageAt = incomingMessage.createdAt,
                                    snippet = if (incomingMessage.isSystem) "📢 Tin nhắn hệ thống" else incomingMessage.body
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
                                        name = updatedDto.name ?: it.name,
                                        // walletId  = updatedDto.walletId ?: it.walletId,
                                        avatarUrl = updatedDto.avatarUrl ?: it.avatarUrl
                                    )
                                )
                            }
                        }
                    }
                }

//                channelScope.launch {
//                    walletUpdateFlow.collect {
//                        _walletUpdateSignal.tryEmit(conversationId)
//                    }
//                }
//
//                channelScope.launch {
//                    transactionInsertFlow.collect {
//                        _walletUpdateSignal.tryEmit(conversationId)
//                    }
//                }

                channel.subscribe()
                // Holding the per-conversation lock, no other op can be mid-flight, so register
                // unconditionally; unsubscribe/reconnect only run after we release the lock.
                activeChannels[conversationId] = ChannelHandle(channel, channelScope)
                reconnectBackoff.remove(conversationId)
                deferred.complete(Unit)
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
                Timber.tag("Chat").e(e, "subscribe failed for $conversationId")
                handleRealtimeReconnect(conversationId)
            } finally {
                mutex.unlock()
            }
        }
    }

    /** Cancels this conversation's collectors and unsubscribes its channel. Safe to call
     *  when leaving ChatScreen; a no-op if the conversation was never subscribed. */
    fun unsubscribeFromChatRealtime(conversationId: String) {
        reconnectingChannels.remove(conversationId)
        reconnectBackoff.remove(conversationId)
        repositoryScope.launch {
            // Wait for any in-flight subscribe to finish before tearing down, so we never miss a
            // channel that is mid-registration (which would otherwise leak).
            channelMutex(conversationId).withLock {
                subscribeDeferreds.remove(conversationId)
                val handle = activeChannels.remove(conversationId) ?: return@withLock
                handle.scope.cancel()
                runCatching { handle.channel.unsubscribe() }
            }
        }
    }

    /**
     * Tears down all chat realtime state so an in-process logout / account switch starts clean.
     * Cancels every per-conversation and the global channel's collectors, clears the channel
     * maps, and resets the global-channel user guard so the next login (any account) rebuilds
     * its channels from scratch. The user guard is reset synchronously; the socket unsubscribes
     * run off the caller's path. Call before [AuthRepository.signOut] while the session is valid.
     */
    fun resetChatState() {
        val perConversation = activeChannels.values.toList()
        activeChannels.clear()
        reconnectingChannels.clear()
        reconnectBackoff.clear()
        subscribeDeferreds.clear()

        val global = synchronized(globalChannelLock) {
            val handle = globalChannelHandle
            globalChannelHandle = null
            globalChannelUserId = null
            handle
        }

        perConversation.forEach { it.scope.cancel() }
        global?.scope?.cancel()
        repositoryScope.launch {
            perConversation.forEach { runCatching { it.channel.unsubscribe() } }
            global?.let { runCatching { it.channel.unsubscribe() } }
        }
    }

    fun subscribeToGlobalConversationsRealtime() {
        val currentUserId = getAuthenticatedUserId()
        if (currentUserId.isBlank()) return
        // The repo is a singleton and every ChatViewModel calls this. Keep exactly one global
        // channel for the current user; on an in-process account switch, tear the previous
        // user's channel down and rebuild so the new user actually receives list realtime.
        synchronized(globalChannelLock) {
            if (globalChannelUserId == currentUserId && globalChannelHandle != null) return
            globalChannelHandle?.let { stale ->
                stale.scope.cancel()
                repositoryScope.launch { runCatching { stale.channel.unsubscribe() } }
            }
            globalChannelHandle = null
            globalChannelUserId = currentUserId
        }

        val globalScope = CoroutineScope(
            SupervisorJob(repositoryScope.coroutineContext[Job]) + Dispatchers.IO + realtimeErrorHandler
        )
        globalScope.launch {
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

                globalScope.launch {
                    participantInsertFlow.collect { action ->
                        val userIdStr =
                            action.record["user_id"]?.toString()?.replace("\"", "")?.lowercase()
                        if (userIdStr == currentUserId) fetchAndSaveConversationsToLocal()
                    }
                }

                globalScope.launch {
                    participantDeleteFlow.collect { action ->
                        val userIdStr =
                            action.oldRecord["user_id"]?.toString()?.replace("\"", "")?.lowercase()
                        val convIdStr =
                            action.oldRecord["conversation_id"]?.toString()?.replace("\"", "")
                        if (userIdStr == currentUserId) {
                            if (convIdStr != null) chatDAO.deleteConversationById(convIdStr)
                            fetchAndSaveConversationsToLocal()
                        }
                    }
                }

                // Read elsewhere: another device advanced last_read_at — mirror it locally so
                // the list de-emphasises a conversation the user already read.
                globalScope.launch {
                    participantUpdateFlow.collect { action ->
                        val rec = action.record
                        val userIdStr = rec["user_id"]?.toString()?.replace("\"", "")?.lowercase()
                        if (userIdStr == currentUserId) {
                            val convIdStr = rec["conversation_id"]?.toString()?.replace("\"", "")
                            val lastRead =
                                rec["last_read_at"]?.toString()?.replace("\"", "")?.toLongOrNull()
                            if (convIdStr != null && lastRead != null) {
                                chatDAO.markConversationRead(convIdStr, currentUserId, lastRead)
                            }
                        }
                    }
                }

                globalScope.launch {
                    messageInsertFlow.collect { action ->
                        val dto = action.decodeRecord<SupabaseMessageDto>()
                        val exist = chatDAO.getMessageByServerId(dto.id ?: "")
                        if (exist == null) {
                            val incoming = Message(
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
                            chatDAO.insertMessage(incoming)
                            chatDAO.updateLastMessage(
                                conversationId = incoming.conversationId,
                                messageAt = incoming.createdAt,
                                snippet = if (incoming.isSystem) "📢 Tin nhắn hệ thống" else incoming.body
                            )
                        }
                    }
                }

                // subscribe() after collectors — same rule as the chat channel above.
                globalChannel.subscribe()
                val superseded = synchronized(globalChannelLock) {
                    if (globalChannelUserId == currentUserId) {
                        globalChannelHandle = ChannelHandle(globalChannel, globalScope)
                        false
                    } else {
                        true
                    }
                }
                if (superseded) {
                    // A newer account switch took over mid-subscribe — discard this channel.
                    // unsubscribe() runs on repositoryScope since cancelling globalScope would
                    // abort this suspend call (and can't be invoked inside the lock anyway).
                    globalScope.cancel()
                    repositoryScope.launch { runCatching { globalChannel.unsubscribe() } }
                }
            } catch (e: Exception) {
                synchronized(globalChannelLock) {
                    if (globalChannelUserId == currentUserId) globalChannelUserId = null
                }
                globalScope.cancel()
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
                // Drop any stale channel + collectors before re-subscribing, under the lock so it
                // can't race the subscribe that follows.
                channelMutex(conversationId).withLock {
                    activeChannels.remove(conversationId)?.let { stale ->
                        stale.scope.cancel()
                        runCatching { stale.channel.unsubscribe() }
                    }
                    subscribeDeferreds.remove(conversationId)
                }
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
        val isMember = chatDAO.isUserInConversation(conversationId, getAuthenticatedUserId())
        if (!isMember) {
            Timber.tag("Chat").e("Không thể nhắn tin: Người dùng không thuộc hội thoại này")
            return
        }
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
        chatDAO.updateLastMessage(
            conversationId = conversationId,
            messageAt = pendingMessage.createdAt,
            snippet = if (isSystem) "📢 Tin nhắn hệ thống" else body
        )
        if (performNetworkSend(pendingMessage) == SendResult.PENDING_RETRY) {
            SyncScheduler.enqueueMessageSync(context)
        }
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
                    function = "find_direct_conversation",
                    parameters = buildJsonObject { put("p_friend", targetFriendId) }
                ).decodeAsOrNull<String>()
            }.getOrNull()
            if (!existingId.isNullOrBlank()) return existingId

            val newChatUuid = UUID.randomUUID().toString()
            // Insert via the @Serializable DTO, not a heterogeneous Map<String, Any?>: a mixed
            // String/Boolean/null map has no kotlinx serializer ("Serializer for class 'Any'"),
            // which is why creating a brand-new 1-1 chat failed.
            supabaseClient.from("conversation").insert(
                SupabaseConversationDto(
                    id = newChatUuid,
                    name = null,
                    isGroup = false,
                    walletId = null
                )
            )
            supabaseClient.from("conversation_participant").insert(
                listOf(
                    SupabaseParticipantDto(newChatUuid, currentUserId, false),
                    SupabaseParticipantDto(newChatUuid, targetFriendId, false)
                )
            )
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
                isGroup = true
            )
            chatDAO.insertConversation(localGroup)
            chatDAO.insertParticipantIfAbsent(
                ConversationParticipant(
                    conversationId = groupUuid,
                    userId = currentUserId,
                    isAdmin = true,
                    lastReadAt = System.currentTimeMillis()
                )
            )
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
                        userId = userId,
                        displayName = displayName,
                        avatarUrl = avatarUrl
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

            currentConv?.let {
                chatDAO.insertConversation(it.copy(name = newName))
            }

            sendSystemMessage(conversationId, "Tên nhóm đã được đổi thành '$newName'")
        } catch (e: Exception) {
        }
    }

    /** Upload a new group avatar and update server + local. Other members pick it up via the
     *  conversation realtime UPDATE. Returns false on failure. */
    suspend fun updateGroupAvatar(conversationId: String, imageUri: String): Boolean {
        return try {
            val bytes = context.contentResolver.openInputStream(android.net.Uri.parse(imageUri))
                ?.use { it.readBytes() } ?: return false

            val bucket = supabaseClient.storage.from("chat-images")
            val fileName = "group_avatar_${conversationId}_${UUID.randomUUID()}.jpg"
            bucket.upload(path = fileName, data = bytes) { upsert = true }
            val publicUrl = bucket.publicUrl(fileName)

            supabaseClient.from("conversation").update(mapOf("avatar_url" to publicUrl)) {
                filter { eq("id", conversationId) }
            }
            chatDAO.getConversationById(conversationId).firstOrNull()?.let {
                chatDAO.insertConversation(it.copy(avatarUrl = publicUrl))
            }
            sendSystemMessage(conversationId, "Ảnh đại diện nhóm đã được cập nhật.")
            true
        } catch (e: Exception) {
            Timber.tag("Chat").e(e, "updateGroupAvatar failed")
            false
        }
    }

    /** Remove the group avatar (back to the colored initial). Stores "" rather than NULL so the
     *  server snapshot propagates the cleared value through the fetch merge instead of being
     *  treated as "unchanged"; the UI treats blank as no avatar. */
    suspend fun removeGroupAvatar(conversationId: String): Boolean {
        return try {
            supabaseClient.from("conversation").update(mapOf("avatar_url" to "")) {
                filter { eq("id", conversationId) }
            }
            chatDAO.getConversationById(conversationId).firstOrNull()?.let {
                chatDAO.insertConversation(it.copy(avatarUrl = ""))
            }
            sendSystemMessage(conversationId, "Ảnh đại diện nhóm đã được gỡ.")
            true
        } catch (e: Exception) {
            Timber.tag("Chat").e(e, "removeGroupAvatar failed")
            false
        }
    }

    suspend fun getUserNamesMap(userIds: List<String>): Map<String, String> {
        return try {
            val cleanIds = userIds.map { it.lowercase().trim() }
            if (cleanIds.isEmpty()) return emptyMap()

            val profiles = supabaseClient.from("profile")
                .select(
                    io.github.jan.supabase.postgrest.query.Columns.raw("id, display_name")
                ) { filter { isIn("id", cleanIds) } }
                .decodeList<PeerProfileDto>() // Thay đổi thành PeerProfileDto cho đồng bộ

            // Chuẩn hóa Key thành lowercase và trim để khớp hoàn toàn khi đối chiếu
            profiles.associate { it.id.lowercase().trim() to (it.displayName ?: "Thành viên") }
        } catch (e: Exception) {
            Timber.tag("ChatRepository").e(e, "Lỗi giải mã thông tin tên thành viên mới")
            emptyMap()
        }
    }

    suspend fun addMembersToGroup(conversationId: String, userIds: List<String>) {
        try {
            // 1. Lấy tên hiển thị của những người được thêm
            val cleanIds = userIds.map { it.lowercase().trim() }
            val userNamesMap = getUserNamesMap(cleanIds)

            // 2. Tạo danh sách tên để thông báo (nếu không tìm thấy tên thì dùng "Thành viên")
            val namesJoined = userIds.map { id ->
                userNamesMap[id.lowercase()] ?: "Thành viên"
            }.joinToString(", ")

            // 3. Thực hiện insert vào database
            val participants = userIds.map { userId ->
                SupabaseParticipantDto(conversationId, userId.lowercase(), false)
            }
            supabaseClient.from("conversation_participant").insert(participants)

            // 4. Gửi tin nhắn hệ thống với tên cụ thể
            sendSystemMessage(conversationId, "Đã thêm $namesJoined vào nhóm.")

        } catch (e: Exception) {
            Timber.e(e, "Lỗi thêm thành viên vào nhóm")
            throw e
        }
    }

    suspend fun kickMember(conversationId: String, userIdToKick: String, memberName: String) {
        try {
            val currentUserId = getAuthenticatedUserId()
            val targetId = userIdToKick.lowercase().trim()
            // Thực hiện xóa quyền trên Server trước
            supabaseClient.from("conversation_participant").delete {
                filter {
                    eq("conversation_id", conversationId)
                    eq("user_id", targetId)
                }
            }
            // NẾU LÀ CHÍNH MÌNH RỜI NHÓM
            if (targetId == currentUserId) {
                // Gửi tin nhắn lên server trước để tài khoản khác thấy
                sendSystemMessage(conversationId, "$memberName đã rời khỏi nhóm.")
                kotlinx.coroutines.delay(300) // Chờ một chút để tin kịp đẩy lên
                deleteConversationLocal(conversationId) // Xóa local máy mình
                return // BẮT BUỘC RETURN: Không gửi tin nhắn hệ thống nữa để tránh tự tạo lại phòng chat rác
            }

            // NẾU LÀ MỜI NGƯỜI KHÁC RA
            sendSystemMessage(conversationId, "Đã mời $memberName rời khỏi nhóm.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun disbandGroup(conversationId: String) {
        try {
            // Gửi tin nhắn hệ thống thông báo giải tán trước
            sendSystemMessage(conversationId, "Trưởng nhóm đã giải tán nhóm.")

            // Xóa sạch dữ liệu trên Server
            supabaseClient.from("message").delete { filter { eq("conversation_id", conversationId) } }
            supabaseClient.from("conversation_participant").delete { filter { eq("conversation_id", conversationId) } }
            supabaseClient.from("conversation").delete { filter { eq("id", conversationId) } }

            // Xóa sạch Local lập tức
            deleteConversationLocal(conversationId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    suspend fun deleteOneToOneChat(conversationId: String) {
        try {
            val currentUserId = getAuthenticatedUserId()
            if (currentUserId.isBlank()) return

            // 1. XÓA TRÊN SERVER: Gỡ tư cách tham gia của chính mình trong phòng chat này trên Supabase
            supabaseClient.from("conversation_participant").delete {
                filter {
                    eq("conversation_id", conversationId)
                    eq("user_id", currentUserId)
                }
            }

            // 2. XÓA DƯỚI LOCAL: Dọn sạch bộ nhớ Room DB local dưới máy mình
            deleteConversationLocal(conversationId)

            Timber.tag("Chat").d("Xóa cuộc trò chuyện 1-1 thành công trên cả Server và Local")
        } catch (e: Exception) {
            Timber.tag("Chat").e(e, "deleteOneToOneChat failed")
            e.printStackTrace()
        }
    }
    suspend fun sendSystemMessage(conversationId: String, content: String) {
        sendMessage(
            conversationId = conversationId,
            senderId = getAuthenticatedUserId(),
            body = content,
            imageUrl = null,
            isSystem = true
        )
    }

    suspend fun deleteConversationLocal(conversationId: String) {
        try {
            chatDAO.deleteParticipantsByConversation(conversationId)
            chatDAO.deleteConversationById(conversationId)
            chatDAO.deleteMessagesByConversation(conversationId)
        } catch (e: Exception) {
            Timber.e(e, "Lỗi khi dọn dẹp dữ liệu local phòng chat")
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
                    chatDAO.updateLastMessage(
                        conversationId = newLocalMessage.conversationId,
                        messageAt = newLocalMessage.createdAt,
                        snippet = newLocalMessage.body
                    )
                }
            }
        } catch (e: Exception) {
            // Bắt lỗi để dọn sạch local phòng chat
            if (e.message?.contains("policy", ignoreCase = true) == true || e.toString().contains("PostgrestException")) {
                deleteConversationLocal(conversationId)
            }
        }
    }

    private enum class SendResult { SENT, PENDING_RETRY, FAILED }

    private suspend fun performNetworkSend(message: Message): SendResult {
        return try {
            var finalImageUrl = message.imageUrl
            val currentUserId = getAuthenticatedUserId()

            if (message.imageUrl != null && (message.imageUrl.startsWith("content://") || message.imageUrl.startsWith(
                    "file://"
                ))
            ) {
                // Read + decode + downscale + JPEG-compress are heavy; the online send path is
                // launched on Dispatchers.Main, so do them off it (a 12MP photo would otherwise
                // block the UI for hundreds of ms). Downscale fixes the slow load: gallery photos
                // are multi-MB but a chat bubble only shows a ~150dp thumbnail.
                val uploadBytes = withContext(Dispatchers.IO) {
                    val raw = try {
                        context.contentResolver.openInputStream(android.net.Uri.parse(message.imageUrl))
                            ?.use { it.readBytes() }
                    } catch (readErr: Exception) {
                        Timber.tag("Chat").e(readErr, "queued image no longer readable")
                        null
                    }
                    raw?.let { downscaleForUpload(it) }
                }
                if (uploadBytes == null) {
                    // Local image is gone/unreadable (e.g. a GetContent content:// grant lost after
                    // process death). That's permanent, not a connectivity issue — fail it so the
                    // worker doesn't retry forever and block the rest of the PENDING queue.
                    chatDAO.updateMessage(message.copy(syncStatus = MessageSyncStatus.FAILED))
                    return SendResult.FAILED
                }
                val storageBucket = supabaseClient.storage.from("chat-images")
                val fileName = "${UUID.randomUUID()}.jpg"
                storageBucket.upload(path = fileName, data = uploadBytes) { upsert = true }
                finalImageUrl = storageBucket.publicUrl(fileName)
            }

            val finalSenderId = currentUserId

            // message.localId doubles as the server row id, so a resend is the same row.
            val payload = SupabaseMessageDto(
                id = message.localId,
                conversationId = message.conversationId,
                senderId = finalSenderId,
                body = message.body,
                imageUrl = finalImageUrl,
                isSystem = message.isSystem,
                createdAt = Clock.System.now().toString()
            )

            // Broadcast is best-effort instant delivery when a channel is live; it's silently
            // skipped in the background worker, where no channel is subscribed.
            if (awaitChannelReady(message.conversationId, 2000)) {
                activeChannels[message.conversationId]?.channel?.let { channel ->
                    runCatching {
                        channel.broadcast(
                            event = BROADCAST_EVENT_NEW_MESSAGE,
                            message = payload
                        )
                    }
                }
            }

            // upsert (not insert) keyed on the deterministic local id: retrying a message the
            // server already stored is a no-op update, not a duplicate-key failure.
            val response = supabaseClient.from("message").upsert(listOf(payload)) { select() }
            val insertedDto = response.decodeSingle<SupabaseMessageDto>()

            chatDAO.updateMessage(
                message.copy(
                    syncStatus = MessageSyncStatus.SENT,
                    serverId = insertedDto.id,
                    imageUrl = finalImageUrl,
                    senderId = finalSenderId,
                    createdAt = parseServerTimeToLong(insertedDto.createdAt)
                )
            )
            SendResult.SENT
        } catch (e: Exception) {
            val offline = runCatching { !networkMonitor.isOnline.first() }.getOrDefault(false)
            if (offline || e.isNetworkError()) {
                // Connectivity problem: keep the message queued for the worker, never FAILED.
                chatDAO.updateMessage(message.copy(syncStatus = MessageSyncStatus.PENDING))
                SendResult.PENDING_RETRY
            } else {
                Timber.tag("Chat").e(e, "message send failed (server error)")
                chatDAO.updateMessage(message.copy(syncStatus = MessageSyncStatus.FAILED))
                SendResult.FAILED
            }
        }
    }

    private fun Throwable.isNetworkError(): Boolean {
        var cause: Throwable? = this
        while (cause != null) {
            if (cause is java.io.IOException) return true
            cause = cause.cause
        }
        return false
    }

    /** Decode, fit within [maxDim] px, and JPEG-compress an image so chat uploads stay small.
     *  Falls back to the original bytes if anything goes wrong (unsupported/corrupt format). */
    private fun downscaleForUpload(bytes: ByteArray, maxDim: Int = 1280, quality: Int = 80): ByteArray {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return bytes

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxDim || bounds.outHeight / (sample * 2) >= maxDim) {
                sample *= 2
            }
            val decoded = BitmapFactory.decodeByteArray(
                bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }
            ) ?: return bytes

            // Re-encoding to JPEG drops the EXIF orientation tag, and BitmapFactory never applies
            // it — so bake the rotation into the pixels, otherwise rotated camera photos (which the
            // old raw upload kept upright via EXIF) would now display sideways.
            val rotation = runCatching {
                when (ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }.getOrDefault(0f)

            val scale = maxDim.toFloat() / maxOf(decoded.width, decoded.height)
            val matrix = Matrix().apply {
                if (rotation != 0f) postRotate(rotation)
                if (scale < 1f) postScale(scale, scale)
            }
            val transformed = if (rotation != 0f || scale < 1f) {
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            } else {
                decoded
            }

            val out = ByteArrayOutputStream()
            transformed.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (transformed != decoded) transformed.recycle()
            decoded.recycle()
            out.toByteArray()
        } catch (e: Exception) {
            Timber.tag("Chat").w(e, "image downscale failed; uploading original")
            bytes
        }
    }

    /**
     * Drains the PENDING message queue oldest-first (called by [MessageSyncWorker]). Returns
     * false — so the worker retries when connectivity returns — if a send hits a connectivity
     * failure; true when the queue holds no PENDING messages (server-FAILED ones are left for
     * manual retry). Idempotent: [performNetworkSend] upserts by local id.
     */
    suspend fun flushPendingMessages(): Boolean {
        if (getAuthenticatedUserId().isBlank()) return true
        val pending = chatDAO.getPendingMessagesOrdered()
        for (msg in pending) {
            if (performNetworkSend(msg) == SendResult.PENDING_RETRY) return false
        }
        return true
    }

    /** Schedule the offline queue to drain. Call once auth is ready (the worker no-ops when
     *  signed out); WorkManager's CONNECTED constraint also re-runs it when the network returns. */
    fun enqueuePendingMessageSync() {
        SyncScheduler.enqueueMessageSync(context)
    }

    suspend fun retryMessage(message: Message) {
        val reloadingMessage = message.copy(syncStatus = MessageSyncStatus.PENDING)
        chatDAO.updateMessage(reloadingMessage)
        if (performNetworkSend(reloadingMessage) == SendResult.PENDING_RETRY) {
            SyncScheduler.enqueueMessageSync(context)
        }
    }

    suspend fun markAsRead(conversationId: String) {
        val currentUserId = getAuthenticatedUserId()
        if (currentUserId.isBlank()) return

        // Advance the local marker to at least the newest known message: message timestamps
        // come from the server/sender clock, the device clock may lag, and comparing the two
        // would otherwise leave a just-read message counted as unread.
        val now =
            maxOf(System.currentTimeMillis(), chatDAO.getLatestMessageTime(conversationId) ?: 0L)

        chatDAO.insertParticipantIfAbsent(
            ConversationParticipant(
                conversationId = conversationId,
                userId = currentUserId,
                lastReadAt = now
            )
        )
        chatDAO.markConversationRead(conversationId, currentUserId, now)
        // Drop this conversation's server-side unread snapshot so the list badge clears at once.
        _serverUnreadCounts.update { it - conversationId }

        // Server marker advances via a gated RPC (security definer) so clients never need
        // direct UPDATE access to conversation_participant.
        runCatching {
            supabaseClient.postgrest.rpc(
                function = "mark_conversation_read",
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
                        walletId = wallet.id,
                        walletName = wallet.name,
                        role = membership.role
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
        walletId: String,
        amount: Double,
        itemId: String?,
        note: String,
        imageUrl: String? = null,
        conversationId: String? = null
    ): Result<Unit> = runCatching {
        val currentUserId = getAuthenticatedUserId()

        val rpcArgs = PurchaseRpcArgs(
            walletId = walletId,
            amount = amount,
            note = note,
            itemId = null
        )
        supabaseClient.postgrest.rpc(
            function = "rpc_wallet_purchase",
            parameters = rpcArgs
        )

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
                senderId = getAuthenticatedUserId(),
                body = body,
                imageUrl = imageUrl,
                isSystem = true
            )
        }
    }
}