package com.SE114.food_tracker.feature.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SE114.food_tracker.core.designsystem.components.BottomBarContentPadding
import com.SE114.food_tracker.core.designsystem.components.ConfirmDialog
import com.SE114.food_tracker.data.local.dao.FriendItemDto
import com.SE114.food_tracker.data.remote.dto.ProfileDTO
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.feature.friend.components.*
import com.SE114.food_tracker.feature.report.ReportDialog

@Composable
fun FriendScreen(
    viewModel: FriendViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResult by viewModel.searchResult.collectAsStateWithLifecycle()
    val isLoadingSearch by viewModel.isLoadingSearch.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val acceptedFriends by viewModel.acceptedFriends.collectAsStateWithLifecycle()
    val incomingRequests by viewModel.incomingRequests.collectAsStateWithLifecycle()
    val outgoingRequests by viewModel.outgoingRequests.collectAsStateWithLifecycle()
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val profileLoadError by viewModel.profileLoadError.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val busyFriendshipIds by viewModel.busyFriendshipIds.collectAsStateWithLifecycle()
    val isReportSubmitting by viewModel.isReportSubmitting.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var friendPendingDelete by remember { mutableStateOf<FriendItemDto?>(null) }
    var requestPendingDecline by remember { mutableStateOf<FriendItemDto?>(null) }
    var requestPendingCancel by remember { mutableStateOf<FriendItemDto?>(null) }
    var friendPendingReport by remember { mutableStateOf<FriendItemDto?>(null) }

    // Surface failed friend actions as a one-shot snackbar.
    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }
    // Surface a profile-load failure with a retry action (instead of hiding it in the ID slot).
    LaunchedEffect(profileLoadError) {
        profileLoadError?.let {
            val result = snackbarHostState.showSnackbar(message = it, actionLabel = "Thử lại")
            if (result == SnackbarResult.ActionPerformed) viewModel.retryLoadProfile()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FriendScreenContent(
            myProfileId = currentProfile?.id,
            myDisplayName = currentProfile?.displayName?.takeIf { it.isNotBlank() }
                ?: currentProfile?.userId?.takeIf { it.isNotBlank() }
                ?: "Đang tải...",
            myUserId = currentProfile?.userId?.takeIf { it.isNotBlank() }
                ?: if (profileLoadError != null) "Không lấy được ID" else "Đang tải...",
            myAvatarUrl = currentProfile?.avatarUrl,
            searchQuery = searchQuery,
            searchResult = searchResult,
            isLoadingSearch = isLoadingSearch,
            isRefreshing = isRefreshing,
            acceptedFriends = acceptedFriends,
            incomingRequests = incomingRequests,
            outgoingRequests = outgoingRequests,
            busyFriendshipIds = busyFriendshipIds,
            onUpdateSearchQuery = viewModel::updateSearchQuery,
            onRefresh = viewModel::refresh,
            onSendFriendRequest = viewModel::sendFriendRequest,
            onAcceptRequest = viewModel::acceptRequest,
            onDeclineRequest = { request -> requestPendingDecline = request },
            onCancelOutgoingRequest = { request -> requestPendingCancel = request },
            onUnfriend = { friend -> friendPendingDelete = friend },
            onReportFriend = { friend -> friendPendingReport = friend },
            onOpenMyProfile = { currentProfile?.id?.let(onNavigateToProfile) },
            onOpenFriendProfile = { friend -> onNavigateToProfile(friend.userId) },
            onNavigateBack = onNavigateBack
        )
        friendPendingDelete?.let { friend ->
            ConfirmDialog(
                title = "Xóa bạn bè?",
                body = "Bạn có chắc muốn xóa ${friend.displayName} khỏi danh sách bạn bè không?",
                confirmLabel = "Xóa",
                cancelLabel = "Hủy",
                destructive = true,
                onConfirm = {
                    friendPendingDelete = null
                    viewModel.unfriend(friend.friendshipId)
                },
                onDismiss = { friendPendingDelete = null }
            )
        }
        requestPendingDecline?.let { request ->
            ConfirmDialog(
                title = "Từ chối lời mời?",
                body = "Bạn có chắc muốn từ chối lời mời kết bạn từ ${request.displayName} không?",
                confirmLabel = "Từ chối",
                cancelLabel = "Hủy",
                destructive = true,
                onConfirm = {
                    requestPendingDecline = null
                    viewModel.declineRequest(request.friendshipId)
                },
                onDismiss = { requestPendingDecline = null }
            )
        }
        requestPendingCancel?.let { request ->
            ConfirmDialog(
                title = "Hủy lời mời?",
                body = "Bạn có chắc muốn hủy lời mời kết bạn gửi đến ${request.displayName} không?",
                confirmLabel = "Hủy lời mời",
                cancelLabel = "Không",
                destructive = true,
                onConfirm = {
                    requestPendingCancel = null
                    viewModel.cancelOutgoingRequest(request.friendshipId)
                },
                onDismiss = { requestPendingCancel = null }
            )
        }
        friendPendingReport?.let { friend ->
            ReportDialog(
                isSubmitting = isReportSubmitting,
                onDismissRequest = { friendPendingReport = null },
                onConfirmReport = { reason, details ->
                    viewModel.submitReport(friend.userId, reason, details)
                    friendPendingReport = null
                }
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendScreenContent(
    myProfileId: String?,
    myDisplayName: String,
    myUserId: String,
    myAvatarUrl: String?,
    searchQuery: String,
    searchResult: Result<FriendSearchResult>?,
    isLoadingSearch: Boolean,
    isRefreshing: Boolean,
    acceptedFriends: List<FriendItemDto>,
    incomingRequests: List<FriendItemDto>,
    outgoingRequests: List<FriendItemDto>,
    busyFriendshipIds: Set<String>,
    onUpdateSearchQuery: (String) -> Unit,
    onRefresh: () -> Unit,
    onSendFriendRequest: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (FriendItemDto) -> Unit,
    onCancelOutgoingRequest: (FriendItemDto) -> Unit,
    onUnfriend: (FriendItemDto) -> Unit,
    onReportFriend: (FriendItemDto) -> Unit,
    onOpenMyProfile: () -> Unit,
    onOpenFriendProfile: (FriendItemDto) -> Unit,
    onNavigateBack: () -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground),
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = CardWhite,
                color = OrangeMain
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 16.dp,
                start = 16.dp,
                end = 16.dp
            )
        ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onNavigateBack) {
                    Text("Quay lại", color = OrangeMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        item {
            MyTagCard(
                displayName = myDisplayName,
                userId = myUserId,
                avatarUrl = myAvatarUrl,
                onOpenProfile = {
                    if (myProfileId != null) onOpenMyProfile()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            CustomSearchBar(
                query = searchQuery,
                onQueryChange = onUpdateSearchQuery
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isLoadingSearch) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .padding(16.dp),
                    color = OrangeMain
                )
            }
        } else {
            searchResult?.let { result ->
                item {
                    result.onSuccess { search ->
                        SearchResultItem(
                            profile = search.profile,
                            relationship = search.relationship,
                            onSendRequest = onSendFriendRequest
                        )
                    }.onFailure { error ->
                        Text("Không tìm thấy: ${error.message}", color = StatRed, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (incomingRequests.isNotEmpty()) {
            item {
                SectionHeader(title = "Lời mời kết bạn", count = incomingRequests.size)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(incomingRequests, key = { it.friendshipId }) { request ->
                IncomingRequestItem(
                    request = request,
                    isBusy = request.friendshipId in busyFriendshipIds,
                    onAccept = onAcceptRequest,
                    onDecline = { onDeclineRequest(request) }
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        if (outgoingRequests.isNotEmpty()) {
            item {
                SectionHeader(title = "Lời mời đã gửi", count = outgoingRequests.size)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(outgoingRequests, key = { it.friendshipId }) { request ->
                OutgoingRequestItem(
                    request = request,
                    isBusy = request.friendshipId in busyFriendshipIds,
                    onCancel = { onCancelOutgoingRequest(request) }
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        item {
            SectionHeader(title = "Bạn bè", count = acceptedFriends.size)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (acceptedFriends.isEmpty()) {
            item { EmptyFriendState() }
        } else {
            items(acceptedFriends, key = { it.friendshipId }) { friend ->
                FriendListItem(
                    friend = friend,
                    onOpenProfile = { onOpenFriendProfile(friend) },
                    isBusy = friend.friendshipId in busyFriendshipIds,
                    onReport = { onReportFriend(friend) },
                    onUnfriend = { onUnfriend(friend) }
                )
            }
        }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview_EmptyFriendScreen() {
    MaterialTheme {
        FriendScreenContent(
            myProfileId = "profile-tuyn",
            myDisplayName = "tuyn",
            myUserId = "tuyn",
            myAvatarUrl = null,
            searchQuery = "uyen",
            searchResult = Result.success(
                FriendSearchResult(
                    profile = ProfileDTO(
                        id = "profile-uyen",
                        displayName = "u y e n",
                        userId = "uyen",
                        avatarUrl = null
                    ),
                    relationship = FriendRelationship.NONE
                )
            ),
            isLoadingSearch = false,
            isRefreshing = false,
            acceptedFriends = emptyList(),
            incomingRequests = emptyList(),
            outgoingRequests = emptyList(),
            busyFriendshipIds = emptySet(),
            onUpdateSearchQuery = {},
            onRefresh = {},
            onSendFriendRequest = {},
            onAcceptRequest = {},
            onDeclineRequest = {},
            onCancelOutgoingRequest = {},
            onUnfriend = {},
            onReportFriend = {},
            onOpenMyProfile = {},
            onOpenFriendProfile = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun Preview_FilledFriendScreen() {
    MaterialTheme {
        FriendScreenContent(
            myProfileId = "profile-tuyn",
            myDisplayName = "tuyn",
            myUserId = "tuyn",
            myAvatarUrl = null,
            searchQuery = "",
            searchResult = null,
            isLoadingSearch = false,
            isRefreshing = false,
            acceptedFriends = listOf(
                FriendItemDto("1", "profile-tdi", "tdi", "tdi", null, "accepted"),
                FriendItemDto("2", "profile-tzan", "tzan", "tzan", null, "accepted"),
                FriendItemDto("3", "profile-azun", "azun", "azun", null, "accepted")
            ),
            incomingRequests = listOf(
                FriendItemDto("4", "profile-unie", "unie", "unie", null, "pending")
            ),
            outgoingRequests = listOf(
                FriendItemDto("5", "profile-waiting", "waiting", "Pending Friend", null, "pending")
            ),
            busyFriendshipIds = emptySet(),
            onUpdateSearchQuery = {},
            onRefresh = {},
            onSendFriendRequest = {},
            onAcceptRequest = {},
            onDeclineRequest = {},
            onCancelOutgoingRequest = {},
            onUnfriend = {},
            onReportFriend = {},
            onOpenMyProfile = {},
            onOpenFriendProfile = {},
            onNavigateBack = {}
        )
    }
}
