package com.SE114.food_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FriendshipDTO(
    @SerialName("id") val id: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("status") val status: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class FriendshipWriteDTO(
    @SerialName("id") val id: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("status") val status: String
)
