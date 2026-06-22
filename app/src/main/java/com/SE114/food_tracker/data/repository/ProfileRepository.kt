package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.data.local.dao.FriendDAO
import com.SE114.food_tracker.data.local.entities.UserProfileCacheEntity
import com.SE114.food_tracker.data.remote.dto.ProfileDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ProfileRepository @Inject constructor(
    private val friendDao: FriendDAO,
    private val supabaseClient: SupabaseClient
) {
    suspend fun fetchProfile(profileId: String): Result<ProfileDTO> =
        withContext(Dispatchers.IO) {
            runCatching {
                val profile = supabaseClient.postgrest["profile"]
                    .select { filter { eq("id", profileId) } }
                    .decodeSingleOrNull<ProfileDTO>()
                    ?: error("Không tìm thấy người dùng.")

                if (profile.isBanned) {
                    error("Người dùng này không khả dụng.")
                }

                friendDao.insertUserCache(profile.toCacheEntity())
                profile
            }
        }

    suspend fun currentAuthUserId(): String? =
        withContext(Dispatchers.IO) {
            supabaseClient.auth.currentUserOrNull()?.id
        }

    private fun ProfileDTO.toCacheEntity(): UserProfileCacheEntity =
        UserProfileCacheEntity(
            userId = id,
            displayName = displayName?.takeIf { it.isNotBlank() }
                ?: userId?.takeIf { it.isNotBlank() }
                ?: "Người dùng",
            avatarUrl = avatarUrl.orEmpty()
        )
}