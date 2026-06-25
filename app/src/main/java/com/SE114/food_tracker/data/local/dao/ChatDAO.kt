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
 * Conversation with the current user's unread status pre-computed.
 * isUnread = lastMessageAt > lastReadAt (from conversation_participants).
 *
 * Used by ConversationListScreen so UI never has to do timestamp math.
 */
data class ConversationWithUnread(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "is_group") val isGroup: Boolean,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "wallet_id") val walletId: String?,
    @ColumnInfo(name = "last_message_at") val lastMessageAt: Long,
    @ColumnInfo(name = "last_message_snippet") val lastMessageSnippet: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /**
     * true when there is at least one message newer than the user's last read timestamp.
     * SQLite: (lastMessageAt > lastReadAt) is returned as 1/0 from the CASE expression.
     */
    @ColumnInfo(name = "is_unread") val isUnread: Boolean
)

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

    @Query("SELECT * FROM messages WHERE id = :serverId LIMIT 1")
    suspend fun getMessageByServerId(serverId: String): Message?

    // ── CONVERSATION QUERIES ──────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)

    /**
     * All conversations the user participates in, joined with their read timestamp
     * so the UI can show the unread dot without extra computation.
     *
     * Order: conversations with new messages first (lastMessageAt DESC),
     * then by creation date for conversations with no messages yet.
     */
    /* @Query("""
        SELECT
            c.id,
            c.is_group,
            c.name,
            c.wallet_id,
            c.last_message_at,
            c.last_message_snippet,
            c.created_at,
            CASE
                WHEN c.last_message_at > COALESCE(cp.last_read_at, 0) THEN 1
                ELSE 0
            END AS is_unread
        FROM conversations c
        LEFT JOIN conversation_participants cp
            ON cp.conversation_id = c.id AND cp.user_id = :currentUserId
        ORDER BY
            CASE WHEN c.last_message_at > 0 THEN c.last_message_at ELSE c.created_at END DESC
    """)
    fun getAllConversationsWithUnread(currentUserId: String): Flow<List<ConversationWithUnread>> */
    @Query("""
    SELECT 
        c.id, c.is_group, c.name, c.wallet_id, 
        c.last_message_at, c.last_message_snippet, c.created_at,
        CASE WHEN c.last_message_at > COALESCE(cp.last_read_at, 0) THEN 1 ELSE 0 END AS is_unread
    FROM conversations c
    INNER JOIN conversation_participants cp ON c.id = cp.conversation_id
    WHERE cp.user_id = :currentUserId
    GROUP BY c.id -- Tránh trùng lặp
    ORDER BY c.last_message_at DESC
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

    // ── PARTICIPANT / UNREAD QUERIES ──────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ConversationParticipant>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfilesToCache(profiles: List<UserProfileCacheEntity>)

    @Query("""
        UPDATE conversation_participants
        SET last_read_at = :readAt
        WHERE conversation_id = :conversationId AND user_id = :userId
    """)
    suspend fun markConversationRead(conversationId: String, userId: String, readAt: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertParticipantIfAbsent(participant: ConversationParticipant)

}