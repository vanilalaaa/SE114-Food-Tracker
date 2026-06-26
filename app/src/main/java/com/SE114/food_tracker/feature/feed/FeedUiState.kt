package com.SE114.food_tracker.feature.feed

import android.net.Uri
import com.SE114.food_tracker.data.local.dao.FeedCommentDto
import com.SE114.food_tracker.data.local.dao.FeedPostDto
import com.SE114.food_tracker.data.local.dao.FeedSourceItemDto
import com.SE114.food_tracker.data.repository.FeedRepository

const val MaxPostTitleLength = 20
const val MaxPostCaptionLength = 50

data class FeedUiState(
    val currentUserId: String = "",
    val posts: List<FeedPostDto> = emptyList(),
    val sourceItems: List<FeedSourceItemDto> = emptyList(),
    val selectedPostId: String? = null,
    val selectedPostIndex: Int = -1,
    val selectedComments: List<FeedCommentDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = FeedRepository.PAGE_SIZE,
    val isCreateSheetOpen: Boolean = false,
    val selectedSourceItem: FeedSourceItemDto? = null,
    val pickedImageUri: Uri? = null,
    val draftFreeImageTitle: String = "",
    val draftCaption: String = "",
    val draftVisibility: String = FeedVisibility.FRIENDS.value,
    val isLoading: Boolean = false,
    val isCreatingPost: Boolean = false,
    val error: String? = null
) {
    val canLoadMore: Boolean = posts.size >= page * pageSize
    val selectedPost: FeedPostDto? = posts.getOrNull(selectedPostIndex)
}

enum class FeedVisibility(
    val value: String,
    val label: String
) {
    FRIENDS("friends", "Bạn bè"),
    PUBLIC("public", "Công khai"),
    PRIVATE("private", "Riêng tư");

    companion object {
        fun fromValue(value: String): FeedVisibility =
            values().firstOrNull { it.value == value } ?: FRIENDS
    }
}
