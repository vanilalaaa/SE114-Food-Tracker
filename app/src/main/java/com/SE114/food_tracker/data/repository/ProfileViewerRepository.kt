package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.data.local.dao.FriendDAO
import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.dao.ItemDAO
import com.SE114.food_tracker.data.local.dao.LocalProfileSharedItemDto
import com.SE114.food_tracker.data.local.entities.UserProfileCacheEntity
import com.SE114.food_tracker.data.model.ProfileSharedItem
import com.SE114.food_tracker.data.remote.dto.CategoryDTO
import com.SE114.food_tracker.data.remote.dto.ItemDTO
import com.SE114.food_tracker.data.remote.mapper.DataMapper
import com.SE114.food_tracker.data.remote.dto.ProfileDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Singleton
class ProfileViewerRepository @Inject constructor(
    private val friendDao: FriendDAO,
    private val itemDao: ItemDAO,
    private val categoryDao: CategoryDAO,
    private val supabaseClient: SupabaseClient
) {
    suspend fun cachedProfile(profileId: String): ProfileDTO? =
        withContext(Dispatchers.IO) {
            friendDao.getUserCache(profileId)?.toProfileDTO()
        }

    suspend fun cachedSharedItems(ownerId: String): List<ProfileSharedItem> =
        withContext(Dispatchers.IO) {
            itemDao.getSharedItemsForProfile(ownerId).map { it.toProfileSharedItem() }
        }

    suspend fun fetchProfile(profileId: String): Result<ProfileDTO> =
        withContext(Dispatchers.IO) {
            runCatching {
                val profile = supabaseClient.postgrest["profile"]
                    .select(Columns.list("id", "display_name", "user_id", "avatar_url")) {
                        filter { eq("id", profileId) }
                    }
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

                if (categories.isNotEmpty()) {
                    categoryDao.upsertCategoriesFromServer(
                        categories.values.map { with(DataMapper) { it.toEntity() } }
                    )
                }
                if (items.isNotEmpty()) {
                    itemDao.upsertItemsFromServer(items.map { with(DataMapper) { it.toEntity() } })
                }

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
            profileUserId = userId.orEmpty(),
            avatarUrl = avatarUrl.orEmpty()
        )

    private fun UserProfileCacheEntity.toProfileDTO(): ProfileDTO =
        ProfileDTO(
            id = userId,
            displayName = displayName,
            userId = profileUserId,
            avatarUrl = avatarUrl
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
            entryDate = entryDate,
            createdAt = try {
                val formattedInput = if (!createdAt.contains("Z") && !createdAt.contains("+") && createdAt.contains("T")) {
                    "${createdAt}Z"
                } else {
                    createdAt
                }
                val instant = Instant.parse(formattedInput)
                instant.toEpochMilliseconds()
            } catch (e: Exception) {
                e.printStackTrace()
                Clock.System.now().toEpochMilliseconds()
            }
        )

    private fun LocalProfileSharedItemDto.toProfileSharedItem(): ProfileSharedItem =
        ProfileSharedItem(
            itemId = itemId,
            name = name,
            categoryName = categoryName ?: "Khác",
            categoryIcon = categoryIcon.orEmpty(),
            price = price,
            timeLabel = timeType.toProfileTimeLabel(),
            imageUrl = imageUrl,
            entryDate = Instant.fromEpochMilliseconds(entryDateMillis)
                .toLocalDateTime(TimeZone.UTC)
                .date
                .toString(),
            createdAt = createdAt
        )

    private fun Int.toProfileTimeLabel(): String =
        when (this) {
            0 -> "Sáng"
            1 -> "Trưa"
            2 -> "Chiều"
            3 -> "Tối"
            else -> "Khác"
        }
}
