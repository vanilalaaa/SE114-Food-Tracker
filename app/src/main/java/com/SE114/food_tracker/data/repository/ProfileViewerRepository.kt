package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.data.local.dao.FriendDAO
import com.SE114.food_tracker.data.local.entities.UserProfileCacheEntity
import com.SE114.food_tracker.data.model.ProfileSharedItem
import com.SE114.food_tracker.data.remote.dto.CategoryDTO
import com.SE114.food_tracker.data.remote.dto.ItemDTO
import com.SE114.food_tracker.data.remote.dto.ProfileDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ProfileViewerRepository @Inject constructor(
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

                friendDao.insertUserCache(profile.toCacheEntity())
                profile
            }
        }

    suspend fun fetchSharedItems(ownerId: String): Result<List<ProfileSharedItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (supabaseClient.auth.currentUserOrNull() == null) {
                    error("Chưa đăng nhập")
                }

                val items = supabaseClient.postgrest.from("item")
                    .select {
                        filter {
                            eq("owner_id", ownerId)
                            eq("is_shared", true)
                            eq("is_deleted", false)
                        }
                    }
                    .decodeList<ItemDTO>()
                    .sortedWith(
                        compareByDescending<ItemDTO> { it.entryDate }
                            .thenByDescending { it.createdAt }
                    )

                val categories = supabaseClient.postgrest.from("category")
                    .select {
                        filter {
                            eq("is_deleted", false)
                            or {
                                eq("owner_id", ownerId)
                                eq("is_system", true)
                            }
                        }
                    }
                    .decodeList<CategoryDTO>()
                    .associateBy { it.id }

                items.map { dto ->
                    dto.toProfileSharedItem(categories[dto.categoryId])
                }
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

    private fun ItemDTO.toProfileSharedItem(category: CategoryDTO?): ProfileSharedItem =
        ProfileSharedItem(
            itemId = id,
            name = name,
            categoryName = category?.name ?: "Khác",
            categoryIcon = category?.iconUrl.orEmpty(),
            price = price,
            timeLabel = timeType.toProfileTimeLabel(),
            imageUrl = imageUrl,
            entryDate = entryDate
        )

    private fun Int.toProfileTimeLabel(): String =
        when (this) {
            0 -> "Sáng"
            1 -> "Trưa/Chiều"
            2 -> "Tối"
            else -> "Khác"
        }
}
