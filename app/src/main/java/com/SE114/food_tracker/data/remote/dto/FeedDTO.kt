package com.SE114.food_tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedPostRemoteDTO(
    @SerialName("id") val id: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("item_id") val itemId: String? = null,
    @SerialName("caption") val caption: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("visibility") val visibility: String = "friends",
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class FeedLikeRemoteDTO(
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class FeedCommentRemoteDTO(
    @SerialName("id") val id: String,
    @SerialName("post_id") val postId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("body") val body: String,
    @SerialName("created_at") val createdAt: String
)
