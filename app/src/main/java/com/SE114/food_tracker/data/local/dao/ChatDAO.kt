package com.SE114.food_tracker.data.local.dao

import androidx.room.*
import com.SE114.food_tracker.data.local.entities.Conversation
import com.SE114.food_tracker.data.local.entities.ConversationParticipant
import com.SE114.food_tracker.data.local.entities.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDAO {

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

    @Query("SELECT * FROM conversations ORDER BY id DESC")
    fun getAllConversations(): Flow<List<Conversation>>
}