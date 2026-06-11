package com.SE114.food_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BudgetDTO(
    @SerialName("user_id") val userId: String,
    @SerialName("daily") val daily: Double? = null,
    @SerialName("weekly") val weekly: Double? = null,
    @SerialName("monthly") val monthly: Double? = null,
    @SerialName("yearly") val yearly: Double? = null,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)