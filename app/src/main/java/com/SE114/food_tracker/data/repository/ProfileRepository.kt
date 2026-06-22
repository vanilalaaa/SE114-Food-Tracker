package com.SE114.food_tracker.data.repository

import kotlinx.coroutines.flow.Flow

/** Own-profile fields readable under the `authenticated` column grant. */
data class Profile(
    val id: String,
    val displayName: String?,
    val userId: String?,
    val avatarUrl: String?
)

sealed interface ProfileStatus {
    data object Complete : ProfileStatus
    data object Incomplete : ProfileStatus
}

interface ProfileRepository {
    /** Cached current-user profile; refreshed by [refreshMyProfile]. */
    fun observeMyProfile(): Flow<Profile?>

    /** Re-fetches the current user's row into the [observeMyProfile] cache. */
    suspend fun refreshMyProfile(): AuthOutcome<Unit>

    /** Onboarding is complete when `onboarding_completed = true AND user_id IS NOT NULL`. */
    suspend fun getProfileStatus(): AuthOutcome<ProfileStatus>

    /**
     * UPDATEs display_name + user_id (+ avatar when non-null) and sets
     * onboarding_completed; idempotent for the same already-onboarded [userId].
     */
    suspend fun completeOnboarding(displayName: String, userId: String, avatarUrl: String?): AuthOutcome<Unit>

    /** UX-only availability hint; the DB unique index is the final authority. */
    suspend fun isUserIdAvailable(userId: String): AuthOutcome<Boolean>

    /** Own-row UPDATE; [userId]/[avatarUrl] are written only when non-null. */
    suspend fun updateProfile(displayName: String, userId: String?, avatarUrl: String?): AuthOutcome<Unit>

    /** Remaining whole days before user_id can change again; 0 = changeable now. */
    suspend fun userIdCooldownRemaining(): AuthOutcome<Int>
}
