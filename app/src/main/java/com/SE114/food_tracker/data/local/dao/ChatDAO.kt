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

    // Gộp dữ liệu từ bảng user_profile_cache:
    @ColumnInfo(name = "cache_name") val cacheName: String?,
    @ColumnInfo(name = "cache_avatar") val cacheAvatar: String?
)
@Dao
interface ChatDAO {
    // Xử lý độ trễ
    @Query("""
        SELECT m.*, p.display_name AS cache_name, p.avatar_url AS cache_avatar 
        FROM messages m 
        LEFT JOIN user_profile_cache p ON m.sender_id = p.user_id 
        WHERE m.conversation_id = :conversationId 
        ORDER BY m.created_at ASC
    """)
    fun getMessagesWithProfileStream(conversationId: String): Flow<List<MessageWithProfile>>
    // 1. Truy vấn tin nhắn trong cuộc hội thoại (Flow tự động cập nhật realtime lên UI)
    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId ORDER BY created_at ASC")
    fun getMessagesByConversation(conversationId: String): Flow<List<Message>>

    // 2. Chèn tin nhắn mới (khi soạn gửi local hoặc nhận sự kiện từ Realtime Supabase)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    // 3. Cập nhật trạng thái tin nhắn (Chuyển đổi PENDING -> SENT / FAILED)
    @Update
    suspend fun updateMessage(message: Message)

    // 4. Lấy danh sách tin nhắn phục vụ Queue Offline để gửi lại khi có mạng
    @Query("SELECT * FROM messages WHERE sync_status = 'PENDING' OR sync_status = 'FAILED'")
    suspend fun getUnsentMessages(): List<Message>

    // 5. Quản lý danh sách hội thoại
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ConversationParticipant>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfilesToCache(profiles: List<UserProfileCacheEntity>)
    @Query("SELECT * FROM conversations ORDER BY id DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    fun getConversationById(conversationId: String): Flow<Conversation?>

    @Query("SELECT * FROM messages WHERE id = :serverId LIMIT 1")
    suspend fun getMessageByServerId(serverId: String): Message?

}