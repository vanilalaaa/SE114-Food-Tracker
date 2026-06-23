package com.SE114.food_tracker.feature.profile

import com.SE114.food_tracker.data.local.dao.FeedPostDto
import com.SE114.food_tracker.data.model.ProfileSharedItem
import com.SE114.food_tracker.data.remote.dto.ProfileDTO

enum class ProfileTab {
    DIARY,
    POSTS
}

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: ProfileDTO? = null,
    val isSelf: Boolean = false,
    val error: String? = null,
    val selectedTab: ProfileTab = ProfileTab.DIARY,
    val sharedItems: List<ProfileSharedItem> = emptyList(),
    val isDiaryLoading: Boolean = false,
    val diaryError: String? = null,
    val posts: List<FeedPostDto> = emptyList(),
    val isPostsLoading: Boolean = false,
    val isReportSubmitting: Boolean = false,
    val reportMessage: String? = null
) {
    val displayName: String
        get() = profile?.displayName?.takeIf { it.isNotBlank() }
            ?: profile?.userId?.takeIf { it.isNotBlank() }
            ?: "Người dùng"

    val handle: String
        get() = profile?.userId?.takeIf { it.isNotBlank() }?.let { "@$it" }.orEmpty()
}
