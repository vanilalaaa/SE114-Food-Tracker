package com.SE114.food_tracker.feature.auth

import com.SE114.food_tracker.data.repository.AuthOutcome
import com.SE114.food_tracker.data.repository.ProfileRepository
import com.SE114.food_tracker.data.repository.ProfileStatus
import javax.inject.Inject

enum class PostAuthDestination { Diary, CompleteProfile, Admin }

/**
 * Single source of truth for "where do I go once authenticated?". Resolves the
 * server profile status into a destination. A [AuthOutcome.Failure] is propagated
 * unchanged — it is never silently treated as Incomplete or Diary, so callers can
 * keep the user where they are and offer a retry.
 */
class PostAuthNavigator @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend fun resolve(): AuthOutcome<PostAuthDestination> =
        when (val outcome = profileRepository.getProfileStatus()) {
            is AuthOutcome.Success -> AuthOutcome.Success(
                when (outcome.data) {
                    ProfileStatus.Complete -> PostAuthDestination.Diary
                    ProfileStatus.Incomplete -> PostAuthDestination.CompleteProfile
                }
            )
            is AuthOutcome.Failure -> AuthOutcome.Failure(outcome.error)
        }
}
