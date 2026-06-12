package com.SE114.food_tracker.data.local.dao

data class FriendItemDto(
    val friendshipId: String,
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val status: String
)