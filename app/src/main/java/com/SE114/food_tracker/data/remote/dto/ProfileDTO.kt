package com.SE114.food_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDTO(
    @SerialName("id")           val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("user_id")      val userId: String,
    @SerialName("is_banned")    val isBanned: Boolean = false,
    @SerialName("avatar_url") val avatarUrl: String? = null
)
