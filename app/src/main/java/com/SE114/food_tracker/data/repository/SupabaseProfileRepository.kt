package com.SE114.food_tracker.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseProfileRepository @Inject constructor(
    private val client: SupabaseClient
) : ProfileRepository {

    private val auth get() = client.auth
    private val db get() = client.postgrest

    private val myProfile = MutableStateFlow<Profile?>(null)

    @Serializable
    private data class ProfileStatusRow(
        @SerialName("user_id") val userId: String? = null,
        @SerialName("onboarding_completed") val onboardingCompleted: Boolean = false
    )

    @Serializable
    private data class MyProfileRow(
        @SerialName("id") val id: String,
        @SerialName("display_name") val displayName: String? = null,
        @SerialName("user_id") val userId: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null
    )

    override fun observeMyProfile(): Flow<Profile?> = myProfile.asStateFlow()

    override suspend fun refreshMyProfile(): AuthOutcome<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = requireUid()
            val row = db.from("profile")
                .select(Columns.list("id", "display_name", "user_id", "avatar_url")) {
                    filter { eq("id", uid) }
                }
                .decodeList<MyProfileRow>()
                .firstOrNull()
            myProfile.value = row?.let {
                Profile(id = it.id, displayName = it.displayName, userId = it.userId, avatarUrl = it.avatarUrl)
            }
            Unit
        }.fold(
            onSuccess = { AuthOutcome.Success(Unit) },
            onFailure = { AuthOutcome.Failure(it.toAuthError()) }
        )
    }

    override suspend fun getProfileStatus(): AuthOutcome<ProfileStatus> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = requireUid()
            val row = db.from("profile")
                .select(Columns.list("user_id", "onboarding_completed")) {
                    filter { eq("id", uid) }
                }
                .decodeList<ProfileStatusRow>()
                .firstOrNull()
            val complete = row != null && row.onboardingCompleted && !row.userId.isNullOrBlank()
            if (complete) ProfileStatus.Complete else ProfileStatus.Incomplete
        }.fold(
            onSuccess = { AuthOutcome.Success(it) },
            onFailure = { AuthOutcome.Failure(it.toAuthError()) }
        )
    }

    override suspend fun completeOnboarding(
        displayName: String,
        userId: String,
        avatarUrl: String?
    ): AuthOutcome<Unit> = withContext(Dispatchers.IO) {
        val normalized = userId.trim()
        runCatching {
            val uid = requireUid()
            val current = db.from("profile")
                .select(Columns.list("user_id", "onboarding_completed")) { filter { eq("id", uid) } }
                .decodeList<ProfileStatusRow>()
                .firstOrNull()

            val alreadyDone = current != null &&
                current.onboardingCompleted &&
                current.userId?.trim().equals(normalized, ignoreCase = true)
            if (!alreadyDone) {
                db.from("profile").update(
                    {
                        set("display_name", displayName.trim())
                        set("user_id", normalized)
                        set("onboarding_completed", true)
                        avatarUrl?.let { set("avatar_url", it) }
                    }
                ) { filter { eq("id", uid) } }
            }
            Unit
        }.fold(
            onSuccess = {
                refreshMyProfile()
                AuthOutcome.Success(Unit)
            },
            onFailure = { AuthOutcome.Failure(it.toProfileError()) }
        )
    }

    override suspend fun isUserIdAvailable(userId: String): AuthOutcome<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            // RPC (not a direct select): register runs before auth, where RLS hides the
            // profile table. The DB's case-insensitive unique index is the real gate on submit.
            db.rpc("user_id_available", buildJsonObject { put("p_user_id", userId.trim()) })
                .decodeAs<Boolean>()
        }.fold(
            onSuccess = { AuthOutcome.Success(it) },
            onFailure = { AuthOutcome.Failure(it.toAuthError()) }
        )
    }

    override suspend fun getEmailByUserId(userId: String): AuthOutcome<String?> = withContext(Dispatchers.IO) {
        runCatching {
            db.rpc("email_for_user_id", buildJsonObject { put("p_user_id", userId.trim()) })
                .decodeAsOrNull<String>()
        }.fold(
            onSuccess = { AuthOutcome.Success(it) },
            onFailure = { AuthOutcome.Failure(it.toAuthError()) }
        )
    }

    override suspend fun updateProfile(
        displayName: String,
        userId: String?,
        avatarUrl: String?
    ): AuthOutcome<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = requireUid()
            db.from("profile").update(
                {
                    set("display_name", displayName.trim())
                    userId?.let { set("user_id", it.trim()) }
                    avatarUrl?.let { set("avatar_url", it) }
                }
            ) { filter { eq("id", uid) } }
            Unit
        }.fold(
            onSuccess = {
                refreshMyProfile()
                AuthOutcome.Success(Unit)
            },
            onFailure = { AuthOutcome.Failure(it.toProfileError()) }
        )
    }

    override suspend fun userIdCooldownRemaining(): AuthOutcome<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = db.rpc("user_id_cooldown_remaining").decodeAsOrNull<String>()
            parseCooldownDays(raw)
        }.fold(
            onSuccess = { AuthOutcome.Success(it) },
            onFailure = { AuthOutcome.Failure(it.toAuthError()) }
        )
    }

    // The RPC returns a Postgres interval string ("D days HH:MM:SS", or "00:00:00"
    // when elapsed, or null when user_id was never set). Round any remainder up to
    // whole days; 0 means the cooldown is over (or never started).
    private fun parseCooldownDays(raw: String?): Int {
        if (raw.isNullOrBlank()) return 0
        var totalSeconds = 0L
        Regex("""(\d+)\s+day""").find(raw)?.let {
            totalSeconds += it.groupValues[1].toLong() * 86_400L
        }
        Regex("""(\d{1,2}):(\d{2}):(\d{2})""").find(raw)?.let { m ->
            totalSeconds += m.groupValues[1].toLong() * 3_600L +
                m.groupValues[2].toLong() * 60L +
                m.groupValues[3].toLong()
        }
        if (totalSeconds <= 0L) return 0
        return ((totalSeconds + 86_399L) / 86_400L).toInt()
    }

    private fun requireUid(): String =
        auth.currentUserOrNull()?.id ?: error("No authenticated session")
}
