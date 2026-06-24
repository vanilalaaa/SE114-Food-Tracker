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

    @Query("SELECT * FROM friendship WHERE friendship_id = :friendshipId LIMIT 1")
    suspend fun getFriendshipById(friendshipId: String): FriendshipEntity?

    @Query("""
        SELECT * FROM friendship
        WHERE is_deleted = 0
        AND (
            (sender_id = :firstUserId AND receiver_id = :secondUserId)
            OR (sender_id = :secondUserId AND receiver_id = :firstUserId)
        )
        LIMIT 1
    """)
    suspend fun getFriendshipBetween(firstUserId: String, secondUserId: String): FriendshipEntity?

    @Query("""
        SELECT f.friendship_id AS friendshipId, 
               c.user_id AS userId, 
               COALESCE(NULLIF(c.profile_user_id, ''), c.user_id) AS searchUserId,
               c.display_name AS displayName, 
               c.avatar_url AS avatarUrl, 
               f.status AS status
        FROM friendship f
        INNER JOIN user_profile_cache c ON c.user_id = CASE
            WHEN f.sender_id = :myUserId THEN f.receiver_id
            ELSE f.sender_id
        END
        WHERE f.status = 'accepted' 
        AND f.is_deleted = 0 
        AND (f.sender_id = :myUserId OR f.receiver_id = :myUserId)
        ORDER BY lower(c.display_name), c.user_id
    """)
    fun getAcceptedFriends(myUserId: String): Flow<List<FriendItemDto>>

    @Query("""
        SELECT f.friendship_id AS friendshipId, 
               c.user_id AS userId, 
               COALESCE(NULLIF(c.profile_user_id, ''), c.user_id) AS searchUserId,
               c.display_name AS displayName, 
               c.avatar_url AS avatarUrl, 
               f.status AS status
        FROM friendship f
        INNER JOIN user_profile_cache c ON c.user_id = f.sender_id
        WHERE f.receiver_id = :myUserId 
        AND f.status = 'pending' 
        AND f.is_deleted = 0
        ORDER BY f.created_at DESC, f.friendship_id
    """)
    fun getIncomingRequests(myUserId: String): Flow<List<FriendItemDto>>

    @Query("""
        SELECT f.friendship_id AS friendshipId, 
               c.user_id AS userId, 
               COALESCE(NULLIF(c.profile_user_id, ''), c.user_id) AS searchUserId,
               c.display_name AS displayName, 
               c.avatar_url AS avatarUrl, 
               f.status AS status
        FROM friendship f
        INNER JOIN user_profile_cache c ON c.user_id = f.receiver_id
        WHERE f.sender_id = :myUserId 
        AND f.status = 'pending' 
        AND f.is_deleted = 0
        ORDER BY f.created_at DESC, f.friendship_id
    """)
    fun getOutgoingRequests(myUserId: String): Flow<List<FriendItemDto>>

    @Query("UPDATE friendship SET status = :newStatus, sync_status = :syncStatus, updated_at = :updatedAt WHERE friendship_id = :friendshipId")
    suspend fun updateFriendshipStatus(
        friendshipId: String,
        newStatus: String,
        syncStatus: String = "PENDING",
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE friendship SET is_deleted = 1, sync_status = :syncStatus, updated_at = :updatedAt WHERE friendship_id = :friendshipId")
    suspend fun softDeleteFriendship(
        friendshipId: String,
        syncStatus: String = "PENDING",
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE friendship
        SET is_deleted = 1, sync_status = :syncStatus, updated_at = :updatedAt
        WHERE is_deleted = 0
        AND (sender_id = :myUserId OR receiver_id = :myUserId)
        AND friendship_id NOT IN (:remoteFriendshipIds)
    """)
    suspend fun softDeleteFriendshipsMissingFromRemote(
        myUserId: String,
        remoteFriendshipIds: List<String>,
        syncStatus: String = "SYNCED",
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE friendship
        SET is_deleted = 1, sync_status = :syncStatus, updated_at = :updatedAt
        WHERE is_deleted = 0
        AND (sender_id = :myUserId OR receiver_id = :myUserId)
    """)
    suspend fun softDeleteAllFriendshipsForUser(
        myUserId: String,
        syncStatus: String = "SYNCED",
        updatedAt: Long = System.currentTimeMillis()
    )
}
