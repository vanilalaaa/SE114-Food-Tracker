package com.SE114.food_tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.SE114.food_tracker.data.local.entities.FriendshipEntity
import com.SE114.food_tracker.data.local.entities.UserProfileCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriendship(friendship: FriendshipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserCache(userProfile: UserProfileCacheEntity)

    @Query("""
        SELECT f.friendship_id AS friendshipId, 
               c.user_id AS userId, 
               c.display_name AS displayName, 
               c.avatar_url AS avatarUrl, 
               f.status AS status
        FROM friendship f
        INNER JOIN user_profile_cache c ON (c.user_id = f.sender_id OR c.user_id = f.receiver_id)
        WHERE f.status = 'accepted' 
        AND f.is_deleted = 0 
        AND c.user_id != :myUserId
    """)
    fun getAcceptedFriends(myUserId: String): Flow<List<FriendItemDto>>

    @Query("""
        SELECT f.friendship_id AS friendshipId, 
               c.user_id AS userId, 
               c.display_name AS displayName, 
               c.avatar_url AS avatarUrl, 
               f.status AS status
        FROM friendship f
        INNER JOIN user_profile_cache c ON c.user_id = f.sender_id
        WHERE f.receiver_id = :myUserId 
        AND f.status = 'pending' 
        AND f.is_deleted = 0
    """)
    fun getIncomingRequests(myUserId: String): Flow<List<FriendItemDto>>

    @Query("""
        SELECT f.friendship_id AS friendshipId, 
               c.user_id AS userId, 
               c.display_name AS displayName, 
               c.avatar_url AS avatarUrl, 
               f.status AS status
        FROM friendship f
        INNER JOIN user_profile_cache c ON c.user_id = f.receiver_id
        WHERE f.sender_id = :myUserId 
        AND f.status = 'pending' 
        AND f.is_deleted = 0
    """)
    fun getOutgoingRequests(myUserId: String): Flow<List<FriendItemDto>>

    @Query("UPDATE friendship SET status = :newStatus, sync_status = 'PENDING', updated_at = :updatedAt WHERE friendship_id = :friendshipId")
    suspend fun updateFriendshipStatus(friendshipId: String, newStatus: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE friendship SET is_deleted = 1, sync_status = 'PENDING', updated_at = :updatedAt WHERE friendship_id = :friendshipId")
    suspend fun softDeleteFriendship(friendshipId: String, updatedAt: Long = System.currentTimeMillis())
}