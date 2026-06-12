package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.data.local.dao.FriendDAO
import com.SE114.food_tracker.data.local.dao.FriendItemDto
import com.SE114.food_tracker.data.local.entities.FriendshipEntity
import com.SE114.food_tracker.data.local.entities.UserProfileCacheEntity
import com.SE114.food_tracker.data.remote.dto.ProfileDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepository @Inject constructor(
    private val friendDao: FriendDAO,
    private val supabaseClient: SupabaseClient
) {
    private val currentUserId = "my_user_id_mock"

    fun getAcceptedFriends(): Flow<List<FriendItemDto>> {
        return friendDao.getAcceptedFriends(currentUserId)
    }

    fun getIncomingRequests(): Flow<List<FriendItemDto>> {
        return friendDao.getIncomingRequests(currentUserId)
    }

    fun getOutgoingRequests(): Flow<List<FriendItemDto>> {
        return friendDao.getOutgoingRequests(currentUserId)
    }

    suspend fun acceptRequest(friendshipId: String) {
        friendDao.updateFriendshipStatus(friendshipId, "accepted")
    }

    suspend fun declineRequest(friendshipId: String) {
        friendDao.updateFriendshipStatus(friendshipId, "declined")
    }

    suspend fun unfriend(friendshipId: String) {
        friendDao.softDeleteFriendship(friendshipId)
    }

    suspend fun searchUser(searchUserId: String): Result<ProfileDTO> {
        return try {
            if (searchUserId == currentUserId) {
                return Result.failure(Exception("Không thể tự kết bạn với chính mình!"))
            }

            val profile = supabaseClient.postgrest["profile"]
                .select { filter { eq("user_id", searchUserId) } }
                .decodeSingleOrNull<ProfileDTO>()

            if (profile != null && !profile.isBanned) {
                val cacheEntity = UserProfileCacheEntity(
                    userId = profile.id,
                    displayName = profile.displayName,
                    avatarUrl = profile.avatarUrl ?: ""
                )
                friendDao.insertUserCache(cacheEntity)
                Result.success(profile)
            } else {
                Result.failure(Exception("Không tìm thấy người dùng này!"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendFriendRequest(targetProfileId: String): Result<Unit> {
        return try {
            val friendshipId = UUID.randomUUID().toString()
            val entity = FriendshipEntity(
                friendshipId = friendshipId,
                senderId = currentUserId,
                receiverId = targetProfileId,
                status = "pending",
                syncStatus = "PENDING"
            )
            friendDao.insertFriendship(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}