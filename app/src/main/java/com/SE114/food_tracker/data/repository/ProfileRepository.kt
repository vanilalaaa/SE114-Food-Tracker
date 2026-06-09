package com.SE114.food_tracker.data.repository

sealed interface ProfileStatus {
    data object Complete : ProfileStatus
    data object Incomplete : ProfileStatus
}

interface ProfileRepository {
    /** Onboarding is complete when `onboarding_completed = true AND user_id IS NOT NULL`. */
    suspend fun getProfileStatus(): AuthOutcome<ProfileStatus>

    /** UPDATEs the current user's row; idempotent for the same normalized [userId]. */
    suspend fun completeOnboarding(userId: String): AuthOutcome<Unit>

    /** UX-only availability hint; the DB unique index is the final authority. */
    suspend fun isUserIdAvailable(userId: String): AuthOutcome<Boolean>
}
