package com.SE114.food_tracker.data.local.dao

data class FeedPostDto(
    val postId: String,
    val ownerId: String,
    val ownerName: String,
    val itemId: String?,
    val itemName: String?,
    val imageUrl: String,
    val caption: String,
    val visibility: String,
    val likeCount: Int,
    val commentCount: Int,
    val isLikedByMe: Boolean,
    val createdAt: Long
)

data class FeedCommentDto(
    val commentId: String,
    val postId: String,
    val userId: String,
    val displayName: String,
    val body: String,
    val createdAt: Long
)

data class FeedSourceItemDto(
    val itemId: String,
    val name: String,
    val imageUrl: String?,
    val price: Double,
    val entryDate: Long
)
