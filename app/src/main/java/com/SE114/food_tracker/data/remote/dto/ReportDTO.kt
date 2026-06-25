package com.SE114.food_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReportDTO(
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("target_id") val targetId: String,
    @SerialName("reason") val reason: String,
    @SerialName("status") val status: String = "pending"
)
