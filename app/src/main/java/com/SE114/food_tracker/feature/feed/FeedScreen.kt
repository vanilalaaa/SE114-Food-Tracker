package com.SE114.food_tracker.feature.feed

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.CardWhite
import com.SE114.food_tracker.core.designsystem.theme.MainBackground
import com.SE114.food_tracker.core.designsystem.theme.TextPrimary
import com.SE114.food_tracker.data.local.dao.FeedPostDto
import com.SE114.food_tracker.data.local.dao.FeedSourceItemDto
import com.SE114.food_tracker.feature.diary.components.AddActionButton
import com.SE114.food_tracker.feature.feed.components.FeedComposerSheet
import com.SE114.food_tracker.feature.feed.components.FeedGridContent
import com.SE114.food_tracker.feature.feed.components.FeedPagingEffect
import com.SE114.food_tracker.feature.feed.components.FeedPostDetailOverlay
import java.io.File

@Composable
fun FeedScreen(
    onNavigateToFriend: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onImagePicked(uri)
            viewModel.openCreateSheet()
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            pendingCameraUri?.let { uri ->
                viewModel.onImagePicked(uri)
                viewModel.openCreateSheet()
            }
        }
    }

    FeedScreenContent(
        uiState = uiState,
        onNavigateToFriend = onNavigateToFriend,
        onNavigateToProfile = onNavigateToProfile,
        onPostClick = viewModel::openPostDetail,
        onRefresh = viewModel::refresh,
        onLoadNextPage = viewModel::loadNextPage,
        onOpenComposer = viewModel::openCreateSheet,
        onCloseComposer = viewModel::closeCreateSheet,
        onPickImage = { imagePicker.launch("image/*") },
        onTakePhoto = {
            runCatching { createFeedCameraUri(context) }
                .onSuccess { uri ->
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
                }
                .onFailure { viewModel.showError("Không mở được camera") }
        },
        onSelectSourceItem = viewModel::selectSourceItem,
        onFreeImageTitleChange = viewModel::updateDraftFreeImageTitle,
        onCaptionChange = viewModel::updateDraftCaption,
        onVisibilityChange = viewModel::updateDraftVisibility,
        onCreatePost = viewModel::createPost,
        onClearError = viewModel::clearError,
        onClosePostDetail = viewModel::closePostDetail,
        onSelectPostAt = viewModel::selectPostAt,
        onToggleLike = viewModel::toggleLike,
        onDeletePost = viewModel::deletePost,
        onAddComment = viewModel::addComment
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreenContent(
    uiState: FeedUiState,
    onNavigateToFriend: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit,
    onOpenComposer: () -> Unit,
    onCloseComposer: () -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onSelectSourceItem: (FeedSourceItemDto?) -> Unit,
    onFreeImageTitleChange: (String) -> Unit,
    onCaptionChange: (String) -> Unit,
    onVisibilityChange: (FeedVisibility) -> Unit,
    onCreatePost: () -> Unit,
    onClearError: () -> Unit,
    onClosePostDetail: () -> Unit,
    onSelectPostAt: (Int) -> Unit,
    onToggleLike: (String) -> Unit,
    onDeletePost: (String) -> Unit,
    onAddComment: (String, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(uiState.error, uiState.selectedPostId) {
        val error = uiState.error
        if (error != null && uiState.selectedPostId == null) {
            snackbarHostState.showSnackbar(error)
            onClearError()
        }
    }

    FeedPagingEffect(
        gridState = gridState,
        uiState = uiState,
        onLoadNextPage = onLoadNextPage
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MainBackground)
    ) {
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = uiState.isLoading,
                    state = pullToRefreshState,
                    containerColor = CardWhite,
                    color = TextPrimary
                )
            }
        ) {
            FeedGridContent(
                uiState = uiState,
                gridState = gridState,
                onNavigateToFriend = onNavigateToFriend,
                onPostClick = onPostClick
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            AddActionButton(
                onClick = onOpenComposer,
                contentDescription = "Tạo bài viết"
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 92.dp)
        )

        if (uiState.isCreateSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = onCloseComposer,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MainBackground
            ) {
                FeedComposerSheet(
                    uiState = uiState,
                    onPickImage = onPickImage,
                    onTakePhoto = onTakePhoto,
                    onSelectSourceItem = onSelectSourceItem,
                    onFreeImageTitleChange = onFreeImageTitleChange,
                    onCaptionChange = onCaptionChange,
                    onVisibilityChange = onVisibilityChange,
                    onCreatePost = onCreatePost,
                    onCancel = onCloseComposer,
                    onClearError = onClearError
                )
            }
        }

        FeedPostDetailOverlay(
            uiState = uiState,
            onClose = onClosePostDetail,
            onNavigateToProfile = onNavigateToProfile,
            onSelectPostAt = onSelectPostAt,
            onToggleLike = onToggleLike,
            onDeletePost = onDeletePost,
            onAddComment = onAddComment,
            onClearError = onClearError
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun FeedScreenPreview() {
    FoodTrackerTheme {
        FeedScreenContent(
            uiState = FeedUiState(posts = previewFeedPosts()),
            onNavigateToFriend = {},
            onNavigateToProfile = {},
            onPostClick = {},
            onRefresh = {},
            onLoadNextPage = {},
            onOpenComposer = {},
            onCloseComposer = {},
            onPickImage = {},
            onTakePhoto = {},
            onSelectSourceItem = {},
            onFreeImageTitleChange = {},
            onCaptionChange = {},
            onVisibilityChange = {},
            onCreatePost = {},
            onClearError = {},
            onClosePostDetail = {},
            onSelectPostAt = {},
            onToggleLike = {},
            onDeletePost = {},
            onAddComment = { _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun FeedScreenEmptyPreview() {
    FoodTrackerTheme {
        FeedScreenContent(
            uiState = FeedUiState(),
            onNavigateToFriend = {},
            onNavigateToProfile = {},
            onPostClick = {},
            onRefresh = {},
            onLoadNextPage = {},
            onOpenComposer = {},
            onCloseComposer = {},
            onPickImage = {},
            onTakePhoto = {},
            onSelectSourceItem = {},
            onFreeImageTitleChange = {},
            onCaptionChange = {},
            onVisibilityChange = {},
            onCreatePost = {},
            onClearError = {},
            onClosePostDetail = {},
            onSelectPostAt = {},
            onToggleLike = {},
            onDeletePost = {},
            onAddComment = { _, _, _ -> }
        )
    }
}

private fun previewFeedPosts(): List<FeedPostDto> =
    listOf(
        FeedPostDto(
            postId = "preview-1",
            ownerId = "user-1",
            ownerName = "Thảo Uyên",
            ownerAvatarUrl = null,
            itemId = "item-1",
            itemName = "Matcha latte",
            categoryIconUrl = "🥤",
            imageUrl = "https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=500",
            caption = "Đi học mà vẫn phải có matcha",
            visibility = FeedVisibility.FRIENDS.value,
            likeCount = 18,
            commentCount = 4,
            isLikedByMe = true,
            createdAt = 1_718_000_000_000
        ),
        FeedPostDto(
            postId = "preview-2",
            ownerId = "user-2",
            ownerName = "Bảo Anh",
            ownerAvatarUrl = null,
            itemId = "item-2",
            itemName = "Cơm tấm",
            categoryIconUrl = "🍚",
            imageUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=500",
            caption = "Trưa nay ăn ngon",
            visibility = FeedVisibility.FRIENDS.value,
            likeCount = 9,
            commentCount = 2,
            isLikedByMe = false,
            createdAt = 1_718_000_100_000
        ),
        FeedPostDto(
            postId = "preview-3",
            ownerId = "user-3",
            ownerName = "Minh Quan",
            ownerAvatarUrl = null,
            itemId = null,
            itemName = null,
            categoryIconUrl = null,
            imageUrl = "",
            caption = "Post tự do không có ảnh",
            visibility = FeedVisibility.PUBLIC.value,
            likeCount = 5,
            commentCount = 1,
            isLikedByMe = false,
            createdAt = 1_718_000_200_000
        ),
        FeedPostDto(
            postId = "preview-4",
            ownerId = "user-4",
            ownerName = "Ngọc Hân",
            ownerAvatarUrl = null,
            itemId = "item-4",
            itemName = "Bánh mì",
            categoryIconUrl = "🥖",
            imageUrl = "https://images.unsplash.com/photo-1606755962773-d324e0a13086?w=500",
            caption = "Bữa sáng nhanh gọn",
            visibility = FeedVisibility.FRIENDS.value,
            likeCount = 21,
            commentCount = 6,
            isLikedByMe = true,
            createdAt = 1_718_000_300_000
        )
    )

private fun createFeedCameraUri(context: Context): Uri {
    val tempFile = File.createTempFile(
        "FEED_${System.currentTimeMillis()}_",
        ".jpg",
        context.cacheDir
    ).apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}
