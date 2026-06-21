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
    private data class UserIdRow(@SerialName("user_id") val userId: String? = null)

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

    override suspend fun completeOnboarding(userId: String): AuthOutcome<Unit> = withContext(Dispatchers.IO) {
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
                        set("user_id", normalized)
                        set("onboarding_completed", true)
                    }
                ) { filter { eq("id", uid) } }
            }
            Unit
        }.fold(
            onSuccess = { AuthOutcome.Success(Unit) },
            onFailure = { AuthOutcome.Failure(it.toProfileError()) }
        )
    }

    override suspend fun isUserIdAvailable(userId: String): AuthOutcome<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val normalized = userId.trim()
            // UX-only: a cheap exact-match probe. The DB's case-insensitive unique
            // index is the real gate and surfaces UserIdTaken on submit.
            db.from("profile")
                .select(Columns.list("user_id")) { filter { eq("user_id", normalized) } }
                .decodeList<UserIdRow>()
                .isEmpty()
        }.fold(
            onSuccess = { AuthOutcome.Success(it) },
            onFailure = { AuthOutcome.Failure(it.toAuthError()) }
        )
    }

    private fun requireUid(): String =
        auth.currentUserOrNull()?.id ?: error("No authenticated session")
}
