package com.SE114.food_tracker.feature.profile

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
    val selectedTab: ProfileTab = ProfileTab.DIARY
) {
    val displayName: String
        get() = profile?.displayName?.takeIf { it.isNotBlank() }
            ?: profile?.userId?.takeIf { it.isNotBlank() }
            ?: "Người dùng"

    val handle: String
        get() = profile?.userId?.takeIf { it.isNotBlank() }?.let { "@$it" }.orEmpty()
}