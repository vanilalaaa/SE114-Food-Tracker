package com.SE114.food_tracker.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.MainBackground
import com.SE114.food_tracker.data.model.ProfileSharedItem
import com.SE114.food_tracker.data.remote.dto.ProfileDTO
import com.SE114.food_tracker.feature.profile.components.ProfileDiaryTab
import com.SE114.food_tracker.feature.profile.components.ProfileHeader

@Composable
fun ProfileScreen(
    profileId: String,
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRetry = viewModel::loadProfile,
        onRetryDiary = viewModel::loadSharedDiary
    )
}

@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onRetryDiary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        ProfileHeader(
            uiState = uiState,
            onNavigateBack = onNavigateBack,
            onRetry = onRetry
        )

        if (!uiState.isLoading && uiState.error == null) {
            Spacer(Modifier.height(24.dp))
            ProfileDiaryTab(
                items = uiState.sharedItems,
                isLoading = uiState.isDiaryLoading,
                error = uiState.diaryError,
                onRetry = onRetryDiary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenContentPreview() {
    FoodTrackerTheme {
        ProfileScreenContent(
            uiState = ProfileUiState(
                profile = ProfileDTO(
                    id = "preview-id",
                    displayName = "Thảo Uyên",
                    userId = "uyen_123",
                    avatarUrl = null
                ),
                isSelf = false,
                sharedItems = listOf(
                    ProfileSharedItem(
                        itemId = "1",
                        name = "Phở Bò",
                        categoryName = "Mì & Phở",
                        categoryIcon = "🍜",
                        price = 45_000.0,
                        timeLabel = "Sáng",
                        imageUrl = null,
                        entryDate = "2026-06-07"
                    )
                )
            ),
            onNavigateBack = {},
            onRetry = {},
            onRetryDiary = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenLoadingPreview() {
    FoodTrackerTheme {
        ProfileScreenContent(
            uiState = ProfileUiState(isLoading = true),
            onNavigateBack = {},
            onRetry = {},
            onRetryDiary = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenErrorPreview() {
    FoodTrackerTheme {
        ProfileScreenContent(
            uiState = ProfileUiState(
                error = "Không tải được profile."
            ),
            onNavigateBack = {},
            onRetry = {},
            onRetryDiary = {}
        )
    }
}
