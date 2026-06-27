package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.data.local.dao.FriendDAO
import com.SE114.food_tracker.data.local.dao.FriendItemDto
import com.SE114.food_tracker.data.local.entities.FriendshipEntity
import com.SE114.food_tracker.data.local.entities.UserProfileCacheEntity
import com.SE114.food_tracker.data.remote.dto.FriendshipDTO
import com.SE114.food_tracker.data.remote.dto.FriendshipWriteDTO
import com.SE114.food_tracker.data.remote.dto.ProfileDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FriendRepository @Inject constructor(
    private val friendDao: FriendDAO,
    private val supabaseClient: SupabaseClient
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var friendshipRealtimeChannel: RealtimeChannel? = null
    private val _friendshipRealtimeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val friendshipRealtimeEvents: SharedFlow<Unit> = _friendshipRealtimeEvents.asSharedFlow()

    private val _currentProfile = MutableStateFlow<ProfileDTO?>(null)
    val currentProfile: StateFlow<ProfileDTO?> = _currentProfile

    // Only the columns `authenticated` may read (migration 0001); `select *` would hit the
    // ungranted sensitive columns (is_banned, …) and fail with a permission error.
    private val profileColumns = Columns.list("id", "display_name", "user_id", "avatar_url")

    fun getAcceptedFriends(): Flow<List<FriendItemDto>> =
        currentProfileScoped { profile -> friendDao.getAcceptedFriends(profile.id) }

    fun getIncomingRequests(): Flow<List<FriendItemDto>> =
        currentProfileScoped { profile -> friendDao.getIncomingRequests(profile.id) }

    fun getOutgoingRequests(): Flow<List<FriendItemDto>> =
        currentProfileScoped { profile -> friendDao.getOutgoingRequests(profile.id) }

    fun subscribeToFriendshipRealtime() {
        repositoryScope.launch {
            if (friendshipRealtimeChannel != null) return@launch

            runCatching {
                val channel = supabaseClient.channel("friendship_realtime")

                val insertFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "friendship"
                }
                val updateFlow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table = "friendship"
                }
                val deleteFlow = channel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
                    table = "friendship"
                }

                repositoryScope.launch {
                    insertFlow.collect { _friendshipRealtimeEvents.tryEmit(Unit) }
                }
                repositoryScope.launch {
                    updateFlow.collect { _friendshipRealtimeEvents.tryEmit(Unit) }
                }
                repositoryScope.launch {
                    deleteFlow.collect { _friendshipRealtimeEvents.tryEmit(Unit) }
                }

                channel.subscribe()
                friendshipRealtimeChannel = channel
            }
        }
    }

    fun resetFriendshipRealtime() {
        val channel = friendshipRealtimeChannel
        friendshipRealtimeChannel = null
        repositoryScope.launch {
            channel?.let { runCatching { it.unsubscribe() } }
        }
    }

    suspend fun refreshCurrentProfile(): Result<ProfileDTO> = runCatching {
        val profile = fetchCurrentProfile()
        _currentProfile.value = profile
        friendDao.insertUserCache(profile.toCacheEntity())
        profile
    }

    suspend fun refreshFriendships(): Result<Unit> = runCatching {
        val me = requireCurrentProfile()
        val remoteFriendships = supabaseClient.postgrest.from("friendship")
            .select {
                filter {
                    or {
                        eq("sender_id", me.id)
                        eq("receiver_id", me.id)
                    }
                }
            }
            .decodeList<FriendshipDTO>()

        val latestRemoteFriendships = remoteFriendships
            .groupBy { friendshipPairKey(it.senderId, it.receiverId) }
            .values
            .mapNotNull { friendships ->
                friendships.maxWithOrNull(
                    compareBy<FriendshipDTO> { parseCreatedAt(it.createdAt) }
                        .thenBy { it.id }
                )
            }

        latestRemoteFriendships.forEach { dto ->
            val otherProfileId = if (dto.senderId == me.id) dto.receiverId else dto.senderId
            fetchProfileById(otherProfileId)?.let { friendDao.insertUserCache(it.toCacheEntity()) }
            friendDao.insertFriendship(dto.toEntity())
            friendDao.softDeleteOtherFriendshipsBetween(
                firstUserId = dto.senderId,
                secondUserId = dto.receiverId,
                keepFriendshipId = dto.id
            )
        }

        val remoteFriendshipIds = latestRemoteFriendships.map { it.id }
        if (remoteFriendshipIds.isEmpty()) {
            friendDao.softDeleteAllFriendshipsForUser(myUserId = me.id)
        } else {
            friendDao.softDeleteFriendshipsMissingFromRemote(
                myUserId = me.id,
                remoteFriendshipIds = remoteFriendshipIds
            )
        }
    }

    suspend fun acceptRequest(friendshipId: String): Result<Unit> =
        updateRemoteStatus(friendshipId, "accepted")

    suspend fun declineRequest(friendshipId: String): Result<Unit> =
        updateRemoteStatus(friendshipId, "declined")

    suspend fun unfriend(friendshipId: String): Result<Unit> =
        deleteRemoteFriendshipOptimistic(friendshipId)

    suspend fun cancelOutgoingRequest(friendshipId: String): Result<Unit> =
        deleteRemoteFriendshipOptimistic(friendshipId)

    suspend fun searchUser(searchUserId: String): Result<ProfileDTO> = runCatching {
        val me = requireCurrentProfile()
        if (searchUserId == me.userId.orEmpty() || searchUserId == me.id) {
            error("Không thể tự kết bạn với chính mình!")
        }

        val profile = supabaseClient.postgrest["profile"]
            .select(profileColumns) { filter { eq("user_id", searchUserId) } }
            .decodeSingleOrNull<ProfileDTO>()

        // Ban status is server-hidden (not selectable by clients); exclusion of banned
        // accounts is enforced server-side, not here.
        if (profile == null) {
            error("Không tìm thấy người dùng này!")
        }

        friendDao.insertUserCache(profile.toCacheEntity())
        profile
    }

    suspend fun friendshipStatusWith(profileId: String): String? {
        val me = requireCurrentProfile()
        return friendDao.getFriendshipBetween(me.id, profileId)?.status
    }

    suspend fun friendshipWith(profileId: String): FriendshipEntity? {
        val me = requireCurrentProfile()
        return friendDao.getFriendshipBetween(me.id, profileId)
    }

    suspend fun sendFriendRequest(targetProfileId: String): Result<Unit> = runCatching {
        val me = requireCurrentProfile()
        if (targetProfileId == me.id) {
            error("Không thể tự kết bạn với chính mình!")
        }

        val existing = friendDao.getFriendshipBetween(me.id, targetProfileId)
        if (existing != null && existing.status != "declined") {
            error("Đã có lời mời hoặc đã là bạn bè.")
        }
        if (existing != null) {
            deleteRemoteFriendship(existing.friendshipId).getOrThrow()
        }

        val targetProfile = fetchProfileById(targetProfileId)
            ?: error("Không tìm thấy người dùng này!")
        friendDao.insertUserCache(targetProfile.toCacheEntity())

        val reusableFriendship = friendDao.getFriendshipByDirection(me.id, targetProfileId)
        val friendshipId = reusableFriendship?.friendshipId ?: UUID.randomUUID().toString()
        val entity = FriendshipEntity(
            friendshipId = friendshipId,
            senderId = me.id,
            receiverId = targetProfileId,
            status = "pending",
            syncStatus = "PENDING",
            isDeleted = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        friendDao.insertFriendship(entity)
        runCatching {
            supabaseClient.postgrest.from("friendship").upsert(entity.toWriteDto())
            friendDao.updateFriendshipStatus(friendshipId, "pending", syncStatus = "SYNCED")
        }.onFailure { throwable ->
            friendDao.softDeleteFriendship(friendshipId, syncStatus = "FAILED")
            throw throwable
        }.getOrThrow()
    }

    private fun currentProfileScoped(
        block: (ProfileDTO) -> Flow<List<FriendItemDto>>
    ): Flow<List<FriendItemDto>> =
        _currentProfile.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList()) else block(profile)
        }

    private suspend fun requireCurrentProfile(): ProfileDTO =
        _currentProfile.value ?: refreshCurrentProfile().getOrThrow()

    private suspend fun fetchCurrentProfile(): ProfileDTO {
        val authId = currentAuthUserId()

        return fetchProfileById(authId)
            ?: error("Không tìm thấy hồ sơ người dùng hiện tại.")
    }

    private suspend fun currentAuthUserId(): String {
        supabaseClient.auth.currentUserOrNull()?.id?.let { return it }

        val status = withTimeoutOrNull(AUTH_SESSION_WAIT_MS) {
            supabaseClient.auth.sessionStatus.first { it !is SessionStatus.Initializing }
        }

        if (status is SessionStatus.Authenticated) {
            status.session.user?.id?.let { return it }
            supabaseClient.auth.currentUserOrNull()?.id?.let { return it }
        }

        error("Chưa đăng nhập")
    }

    private suspend fun fetchProfileById(profileId: String): ProfileDTO? =
        supabaseClient.postgrest["profile"]
            .select(profileColumns) { filter { eq("id", profileId) } }
            .decodeSingleOrNull<ProfileDTO>()

    private suspend fun fetchFriendshipsBetween(firstUserId: String, secondUserId: String): List<FriendshipDTO> =
        supabaseClient.postgrest.from("friendship")
            .select {
                filter {
                    or {
                        and {
                            eq("sender_id", firstUserId)
                            eq("receiver_id", secondUserId)
                        }
                        and {
                            eq("sender_id", secondUserId)
                            eq("receiver_id", firstUserId)
                        }
                    }
                }
            }
            .decodeList<FriendshipDTO>()

    private suspend fun updateRemoteStatus(friendshipId: String, status: String): Result<Unit> =
        runCatching {
            val friendship = friendDao.getFriendshipById(friendshipId)
                ?: error("Không tìm thấy lời mời kết bạn.")

            val me = requireCurrentProfile()
            if (friendship.senderId != me.id && friendship.receiverId != me.id) {
                error("Không thể cập nhật lời mời này.")
            }

            friendDao.updateFriendshipStatus(friendshipId, status, syncStatus = "PENDING")
            runCatching {
                supabaseClient.postgrest.from("friendship").update(
                    {
                        set("status", status)
                    }
                ) {
                    filter { eq("id", friendshipId) }
                }
                friendDao.updateFriendshipStatus(friendshipId, status, syncStatus = "SYNCED")
            }.onFailure { throwable ->
                friendDao.updateFriendshipStatus(friendshipId, friendship.status, syncStatus = "FAILED")
                throw throwable
            }.getOrThrow()
        }

    private suspend fun deleteRemoteFriendshipOptimistic(friendshipId: String): Result<Unit> =
        runCatching {
            val friendship = friendDao.getFriendshipById(friendshipId)
                ?: error("Không tìm thấy kết bạn.")
            val me = requireCurrentProfile()
            if (friendship.senderId != me.id && friendship.receiverId != me.id) {
                error("Không thể xóa kết bạn này.")
            }

            friendDao.softDeleteFriendship(friendshipId, syncStatus = "PENDING")
            val pairFriendships = fetchFriendshipsBetween(friendship.senderId, friendship.receiverId)
            runCatching {
                pairFriendships.forEach { remoteFriendship ->
                    supabaseClient.postgrest.from("friendship").delete {
                        filter { eq("id", remoteFriendship.id) }
                    }
                }
                val stillExists = fetchFriendshipsBetween(friendship.senderId, friendship.receiverId).isNotEmpty()
                if (stillExists) {
                    error("Chưa xóa được kết bạn trên server.")
                }
                friendDao.softDeleteFriendshipsBetween(
                    firstUserId = friendship.senderId,
                    secondUserId = friendship.receiverId,
                    syncStatus = "SYNCED"
                )
            }.onFailure { throwable ->
                friendDao.restoreFriendshipStatus(
                    friendshipId = friendship.friendshipId,
                    newStatus = friendship.status,
                    syncStatus = "FAILED"
                )
                throw throwable
            }.getOrThrow()
        }

    private suspend fun deleteRemoteFriendship(friendshipId: String): Result<Unit> =
        runCatching {
            val friendship = friendDao.getFriendshipById(friendshipId)
                ?: error("Không tìm thấy kết bạn.")
            val me = requireCurrentProfile()
            if (friendship.senderId != me.id && friendship.receiverId != me.id) {
                error("Không thể xóa kết bạn này.")
            }

            val pairFriendships = fetchFriendshipsBetween(friendship.senderId, friendship.receiverId)
            pairFriendships.forEach { remoteFriendship ->
                supabaseClient.postgrest.from("friendship").delete {
                    filter { eq("id", remoteFriendship.id) }
                }
            }
            val stillExists = fetchFriendshipsBetween(friendship.senderId, friendship.receiverId).isNotEmpty()
            if (stillExists) {
                error("Chưa xóa được kết bạn trên server.")
            }
            friendDao.softDeleteFriendshipsBetween(
                firstUserId = friendship.senderId,
                secondUserId = friendship.receiverId,
                syncStatus = "SYNCED"
            )
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

    private fun FriendshipDTO.toEntity(): FriendshipEntity =
        FriendshipEntity(
            friendshipId = id,
            senderId = senderId,
            receiverId = receiverId,
            status = status,
            syncStatus = "SYNCED",
            createdAt = parseCreatedAt(createdAt),
            updatedAt = System.currentTimeMillis()
        )

    private fun FriendshipEntity.toWriteDto(): FriendshipWriteDTO =
        FriendshipWriteDTO(
            id = friendshipId,
            senderId = senderId,
            receiverId = receiverId,
            status = status
        )

    private fun parseCreatedAt(value: String): Long =
        runCatching { Instant.parse(value).toEpochMilliseconds() }
            .getOrDefault(Clock.System.now().toEpochMilliseconds())

    private fun friendshipPairKey(firstUserId: String, secondUserId: String): String =
        if (firstUserId <= secondUserId) {
            "$firstUserId:$secondUserId"
        } else {
            "$secondUserId:$firstUserId"
        }

    private companion object {
        const val AUTH_SESSION_WAIT_MS = 3_000L
    }
}
