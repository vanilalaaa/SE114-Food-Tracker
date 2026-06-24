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

    /**
     * UX-only availability hint via the `user_id_available` security-definer RPC, so it
     * works before authentication (register) as well as after (onboarding). The DB unique
     * index is the final authority.
     */
    suspend fun isUserIdAvailable(userId: String): AuthOutcome<Boolean>

    /**
     * Resolves the email registered for [userId] (case-insensitive) via the
     * `email_for_user_id` security-definer RPC so a user can log in by handle.
     * Returns null when no profile matches; callers map that to InvalidCredentials.
     */
    suspend fun getEmailByUserId(userId: String): AuthOutcome<String?>

    /** Own-row UPDATE; [userId]/[avatarUrl] are written only when non-null. */
    suspend fun updateProfile(displayName: String, userId: String?, avatarUrl: String?): AuthOutcome<Unit>

    /** Remaining whole days before user_id can change again; 0 = changeable now. */
    suspend fun userIdCooldownRemaining(): AuthOutcome<Int>

    /**
     * Reads the current user's `is_admin` via the `am_i_admin` security-definer RPC
     * (the flag is off the column SELECT grant). Only toggles UI — every admin RPC
     * re-checks server-side.
     */
    suspend fun amIAdmin(): AuthOutcome<Boolean>
}
