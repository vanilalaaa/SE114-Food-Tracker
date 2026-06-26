package com.SE114.food_tracker.data.local.dao

import androidx.room.*
import com.SE114.food_tracker.data.local.entities.Conversation
import com.SE114.food_tracker.data.local.entities.ConversationParticipant
import com.SE114.food_tracker.data.local.entities.Message
import com.SE114.food_tracker.data.local.entities.UserProfileCacheEntity
import kotlinx.coroutines.flow.Flow

// LỚP ĐẠI DIỆN TRUNG GIAN ĐỂ ROOM MAP DATA SAU KHI JOIN
data class MessageWithProfile(
    @ColumnInfo(name = "localId") val id: String?,
    @ColumnInfo(name = "conversation_id") val conversationId: String?,
    @ColumnInfo(name = "sender_id") val senderId: String?,
    val body: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "is_system") val isSystem: Boolean,
    @ColumnInfo(name = "sync_status") val syncStatus: com.SE114.food_tracker.data.local.entities.MessageSyncStatus,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "cache_name") val cacheName: String?,
    @ColumnInfo(name = "cache_avatar") val cacheAvatar: String?
)

/**
 * Conversation row plus the current user's unread state and (for 1-1 chats) the other
 * participant's name/avatar, all pre-computed in SQL so the UI never does timestamp math
 * or extra lookups.
 */
data class ConversationWithUnread(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "is_group") val isGroup: Boolean,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "wallet_id") val walletId: String?,
    /** Group conversation avatar (null for 1-1 — use [peerAvatar] there). */
    @ColumnInfo(name = "avatar_url") val avatarUrl: String? = null,
    @ColumnInfo(name = "last_message_at") val lastMessageAt: Long,
    @ColumnInfo(name = "last_message_snippet") val lastMessageSnippet: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /**
     * Unread = the conversation's last message is newer than the user's read marker. Uses the
     * denormalized last_message_at (fetched for every conversation) so it works even for chats
     * whose messages aren't synced into Room yet (messages only sync on open).
     */
    @ColumnInfo(name = "is_unread") val isUnread: Boolean = false,
    /** Best-effort unread count from locally-synced messages (0 when none are cached yet). */
    @ColumnInfo(name = "unread_count") val unreadCount: Int = 0,
    /** The 1-1 peer's display name; null for groups (use [name] there). */
    @ColumnInfo(name = "peer_name") val peerName: String? = null,
    /** The 1-1 peer's avatar url; null for groups or when not cached yet. */
    @ColumnInfo(name = "peer_avatar") val peerAvatar: String? = null
) {
    /** Name to show in the list: the resolved 1-1 peer name, else the stored conversation name. */
    val displayName: String? get() = peerName ?: name
}

@Dao
interface ChatDAO {

    // ── MESSAGE QUERIES ───────────────────────────────────────────────────────

    @Query("""
        SELECT m.*, p.display_name AS cache_name, p.avatar_url AS cache_avatar 
        FROM messages m 
        LEFT JOIN user_profile_cache p ON m.sender_id = p.user_id 
        WHERE m.conversation_id = :conversationId 
        ORDER BY m.created_at ASC
    """)
    fun getMessagesWithProfileStream(conversationId: String): Flow<List<MessageWithProfile>>

    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId ORDER BY created_at ASC")
    fun getMessagesByConversation(conversationId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Update
    suspend fun updateMessage(message: Message)

    @Query("SELECT * FROM messages WHERE sync_status = 'PENDING' OR sync_status = 'FAILED'")
    suspend fun getUnsentMessages(): List<Message>

    /** Oldest-first PENDING queue the MessageSyncWorker drains when connectivity returns. */
    @Query("SELECT * FROM messages WHERE sync_status = 'PENDING' ORDER BY created_at ASC")
    suspend fun getPendingMessagesOrdered(): List<Message>

    @Query("SELECT * FROM messages WHERE id = :serverId LIMIT 1")
    suspend fun getMessageByServerId(serverId: String): Message?

    @Query("SELECT MAX(created_at) FROM messages WHERE conversation_id = :conversationId")
    suspend fun getLatestMessageTime(conversationId: String): Long?

    // ── CONVERSATION QUERIES ──────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<Conversation>)

    /**
     * All conversations the user participates in, joined with their read timestamp
     * so the UI can show the unread dot without extra computation.
     *
     * Order: conversations with new messages first (lastMessageAt DESC),
     * then by creation date for conversations with no messages yet.
     */
    @Query("""
        SELECT
            c.id,
            c.is_group,
            c.name,
            c.avatar_url,
            c.last_message_at,
            c.last_message_snippet,
            c.created_at,
            CASE WHEN c.last_message_at > COALESCE(cp.last_read_at, 0) THEN 1 ELSE 0 END AS is_unread,
            (SELECT COUNT(*) FROM messages m
                WHERE m.conversation_id = c.id
                  AND m.created_at > COALESCE(cp.last_read_at, 0)
                  AND m.sender_id <> :currentUserId
            ) AS unread_count,
            CASE WHEN c.is_group = 0 THEN (
                SELECT prof.display_name FROM conversation_participants other
                JOIN user_profile_cache prof ON prof.user_id = other.user_id
                WHERE other.conversation_id = c.id AND other.user_id <> :currentUserId
                LIMIT 1
            ) ELSE NULL END AS peer_name,
            CASE WHEN c.is_group = 0 THEN (
                SELECT prof.avatar_url FROM conversation_participants other
                JOIN user_profile_cache prof ON prof.user_id = other.user_id
                WHERE other.conversation_id = c.id AND other.user_id <> :currentUserId
                LIMIT 1
            ) ELSE NULL END AS peer_avatar
        FROM conversations c
        LEFT JOIN conversation_participants cp
            ON cp.conversation_id = c.id AND cp.user_id = :currentUserId
        ORDER BY
            CASE WHEN c.last_message_at > 0 THEN c.last_message_at ELSE c.created_at END DESC
    """)
    fun getAllConversationsWithUnread(currentUserId: String): Flow<List<ConversationWithUnread>>


    /**
     * Plain conversation list (no unread info) — kept for backward-compat with
     * code that doesn't need the unread dot (e.g. GroupWalletScreen).
     */
    @Query("SELECT * FROM conversations ORDER BY id DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    fun getConversationById(conversationId: String): Flow<Conversation?>

    @Query("SELECT * FROM conversations WHERE id IN (:ids)")
    suspend fun getConversationsByIds(ids: List<String>): List<Conversation>

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversationById(conversationId: String)

    @Query("""
        UPDATE conversations
        SET last_message_at      = :messageAt,
            last_message_snippet = :snippet
        WHERE id = :conversationId
          AND (last_message_at < :messageAt OR last_message_at IS NULL)
    """)
    suspend fun updateLastMessage(conversationId: String, messageAt: Long, snippet: String?)
    @Query("DELETE FROM messages WHERE conversation_id = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: String)

    // ── PARTICIPANT / UNREAD QUERIES ──────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ConversationParticipant>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfilesToCache(profiles: List<UserProfileCacheEntity>)

    /** Forward-only: a read marker must never move backwards (guards against clock skew
     *  between device and server, and out-of-order realtime read-sync events). */
    @Query("""
        UPDATE conversation_participants
        SET last_read_at = :readAt
        WHERE conversation_id = :conversationId AND user_id = :userId AND :readAt > last_read_at
    """)
    suspend fun markConversationRead(conversationId: String, userId: String, readAt: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertParticipantIfAbsent(participant: ConversationParticipant)

}