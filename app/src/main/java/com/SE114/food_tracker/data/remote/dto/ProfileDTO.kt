package com.SE114.food_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Own/other-user profile fields readable under the `authenticated` column grant
 * (see migration 0001). Sensitive columns like `is_banned`/`is_admin` are intentionally
 * NOT selectable by clients, so they must not appear here — requesting them makes
 * `select` fail with a permission error.
 */
@Serializable
data class ProfileDTO(
    @SerialName("id")           val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("user_id")      val userId: String? = null,
    @SerialName("avatar_url")   val avatarUrl: String? = null
)
