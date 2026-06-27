package com.SE114.food_tracker.feature.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.components.AppButton
import com.SE114.food_tracker.core.designsystem.components.AppButtonVariant
import com.SE114.food_tracker.core.designsystem.components.AppScaffold
import com.SE114.food_tracker.core.designsystem.components.ConfirmDialog
import com.SE114.food_tracker.core.designsystem.theme.AlertRed
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.data.repository.AdminUser
import com.SE114.food_tracker.feature.admin.components.AdminTopBar
import com.SE114.food_tracker.feature.admin.components.AdminUserRow
import kotlinx.coroutines.flow.distinctUntilChanged

private enum class UserConfirmKind { BAN, DELETE, MAKE_ADMIN, REVOKE_ADMIN }
private data class UserConfirm(val user: AdminUser, val kind: UserConfirmKind)

@Composable
fun AdminUsersScreen(
    onBack: () -> Unit,
    viewModel: AdminUsersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedUser by remember { mutableStateOf<AdminUser?>(null) }
    var pendingConfirm by remember { mutableStateOf<UserConfirm?>(null) }

    val actionErrorText = state.actionError?.adminMessage()
    LaunchedEffect(state.actionError) {
        if (actionErrorText != null) {
            snackbarHostState.showSnackbar(actionErrorText)
            viewModel.clearActionError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AdminUsersContent(
            state = state,
            onBack = onBack,
            onSearchChange = viewModel::onSearchChange,
            onRetry = viewModel::reload,
            onLoadMore = viewModel::loadMore,
            onUserClick = { selectedUser = it }
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }

    selectedUser?.let { user ->
        UserActionSheet(
            user = user,
            deleteRestoreBlocked = viewModel.isDeletionRestoreBlocked(user),
            onDismiss = { selectedUser = null },
            onAdminToggle = {
                selectedUser = null
                pendingConfirm = UserConfirm(
                    user,
                    if (user.isAdmin) UserConfirmKind.REVOKE_ADMIN else UserConfirmKind.MAKE_ADMIN
                )
            },
            onBanToggle = {
                selectedUser = null
                if (user.isBanned) viewModel.setBanned(user, false)
                else pendingConfirm = UserConfirm(user, UserConfirmKind.BAN)
            },
            onDeleteToggle = {
                selectedUser = null
                if (user.isDeleted) viewModel.setDeleted(user, false)
                else pendingConfirm = UserConfirm(user, UserConfirmKind.DELETE)
            }
        )
    }

    pendingConfirm?.let { confirm ->
        val handle = confirm.user.userId ?: confirm.user.displayName.orEmpty()
        when (confirm.kind) {
            UserConfirmKind.BAN -> BanDurationDialog(
                userHandle = handle,
                onPick = { duration ->
                    pendingConfirm = null
                    viewModel.setBanned(confirm.user, true, duration.seconds)
                },
                onDismiss = { pendingConfirm = null }
            )
            UserConfirmKind.DELETE -> ConfirmDialog(
                title        = stringResource(R.string.admin_delete_confirm_title),
                body         = stringResource(R.string.admin_delete_confirm_body, handle),
                confirmLabel = stringResource(R.string.admin_confirm_delete),
                cancelLabel  = stringResource(R.string.admin_cancel),
                destructive  = true,
                onConfirm    = { pendingConfirm = null; viewModel.setDeleted(confirm.user, true) },
                onDismiss    = { pendingConfirm = null }
            )
            UserConfirmKind.MAKE_ADMIN -> ConfirmDialog(
                title        = stringResource(R.string.admin_make_admin_confirm_title),
                body         = stringResource(R.string.admin_make_admin_confirm_body, handle),
                confirmLabel = stringResource(R.string.admin_confirm_make_admin),
                cancelLabel  = stringResource(R.string.admin_cancel),
                onConfirm    = { pendingConfirm = null; viewModel.setAdmin(confirm.user, true) },
                onDismiss    = { pendingConfirm = null }
            )
            UserConfirmKind.REVOKE_ADMIN -> ConfirmDialog(
                title        = stringResource(R.string.admin_revoke_admin_confirm_title),
                body         = stringResource(R.string.admin_revoke_admin_confirm_body, handle),
                confirmLabel = stringResource(R.string.admin_confirm_revoke_admin),
                cancelLabel  = stringResource(R.string.admin_cancel),
                destructive  = true,
                onConfirm    = { pendingConfirm = null; viewModel.setAdmin(confirm.user, false) },
                onDismiss    = { pendingConfirm = null }
            )
        }
    }
}

@Composable
private fun AdminUsersContent(
    state: AdminUsersUiState,
    onBack: () -> Unit,
    onSearchChange: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onUserClick: (AdminUser) -> Unit
) {
    AppScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            AdminTopBar(title = stringResource(R.string.admin_users_title), onBack = onBack)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value         = state.search,
                onValueChange = onSearchChange,
                placeholder   = { Text(stringResource(R.string.admin_users_search_hint)) },
                leadingIcon   = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            val listState = rememberLazyListState()
            LaunchedEffect(listState, state.users.size, state.canLoadMore) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
                    .distinctUntilChanged()
                    .collect { lastVisible ->
                        if (state.canLoadMore && lastVisible >= state.users.lastIndex - 3) onLoadMore()
                    }
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (state.isLoading && state.users.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (state.error != null && state.users.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(state.error.adminMessage(), color = MaterialTheme.colorScheme.error)
                        AppButton(
                            text    = stringResource(R.string.admin_retry),
                            onClick = onRetry,
                            variant = AppButtonVariant.Secondary
                        )
                    }
                } else if (state.users.isEmpty()) {
                    Text(
                        text     = stringResource(R.string.admin_users_empty),
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        state          = listState,
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(state.users, key = { it.id }) { user ->
                            // Thiết kế bao bọc Row để chèn thêm nút Info "i" màu đỏ linh hoạt
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    AdminUserRow(user = user, onClick = { onUserClick(user) })
                                }

                                // Nếu user có trạng thái đặc biệt thì hiển thị nút "i" màu đỏ cùng Bubble
                                if (user.isBanned || user.isDeleted) {
                                    var showBubble by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.padding(end = 4.dp)) {
                                        IconButton(
                                            onClick = { showBubble = true },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Info,
                                                contentDescription = "Xem chi tiết thời gian",
                                                tint = AlertRed
                                            )
                                        }

                                        // Menu thả xuống đóng vai trò làm Bubble thông tin không bị tràn chữ
                                        DropdownMenu(
                                            expanded = showBubble,
                                            onDismissRequest = { showBubble = false },
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            val bubbleText = when {
                                                user.isDeleted -> "Thời gian xóa mềm tự hủy sắp hết hạn vào chu kỳ kế tiếp"
                                                user.isBanned -> "Tài khoản bị khóa. Vui lòng kiểm tra kỹ thời gian kết thúc hoặc lệnh vĩnh viễn trong Sheet."
                                                else -> ""
                                            }
                                            Text(
                                                text = bubbleText,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (state.isLoadingMore) {
                            item {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserActionSheet(
    user: AdminUser,
    deleteRestoreBlocked: Boolean,
    onDismiss: () -> Unit,
    onAdminToggle: () -> Unit,
    onBanToggle: () -> Unit,
    onDeleteToggle: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text  = user.displayName?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.admin_user_no_name),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text  = "@" + (user.userId ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            if (!user.isAdmin) {
                AppButton(
                    text    = stringResource(R.string.admin_action_make_admin),
                    onClick = onAdminToggle,
                    variant = AppButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (user.isBanned) {
                AppButton(
                    text     = stringResource(R.string.admin_action_unban),
                    onClick  = onBanToggle,
                    enabled  = true,
                    variant  = AppButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                AppButton(
                    text    = stringResource(R.string.admin_action_ban),
                    onClick = onBanToggle,
                    variant = AppButtonVariant.Destructive,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (user.isDeleted) {
                AppButton(
                    text     = stringResource(R.string.admin_action_restore),
                    onClick  = onDeleteToggle,
                    enabled  = !deleteRestoreBlocked,
                    variant  = AppButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
                if (deleteRestoreBlocked) {
                    Text(
                        text  = stringResource(R.string.admin_err_deletion_expired),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                AppButton(
                    text    = stringResource(R.string.admin_action_delete),
                    onClick = onDeleteToggle,
                    variant = AppButtonVariant.Destructive,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BanDurationDialog(
    userHandle: String,
    onPick: (BanDuration) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton    = {},
        dismissButton    = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.admin_cancel)) }
        },
        title = { Text(stringResource(R.string.admin_ban_duration_title)) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text  = stringResource(R.string.admin_ban_confirm_body, userHandle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                BanDuration.entries.forEach { duration ->
                    Text(
                        text     = stringResource(duration.labelRes),
                        style    = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(duration) }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun AdminUsersContentPreview() {
    FoodTrackerTheme {
        AdminUsersContent(
            state = AdminUsersUiState(
                users = listOf(
                    AdminUser(
                        id = "1",
                        displayName = "An Nguyễn",
                        userId = "an.nguyen",
                        avatarUrl = null,
                        isAdmin = true,
                        isBanned = false,
                        isDeleted = false
                    ),
                    AdminUser(
                        id = "2",
                        displayName = "shonwannie",
                        userId = "shon",
                        avatarUrl = null,
                        isAdmin = false,
                        isBanned = true,  // Bật cái này để Preview nút "i" màu đỏ
                        isDeleted = false
                    ),
                    AdminUser(
                        id = "3",
                        displayName = "Chi Lê",
                        userId = "chi.le",
                        avatarUrl = null,
                        isAdmin = false,
                        isBanned = false,
                        isDeleted = true  // Hiển thị nút "i" màu đỏ cho tài khoản bị xóa mềm
                    )
                ),
                isLoading = false,
                canLoadMore = false
            ),
            onBack         = {},
            onSearchChange = {},
            onRetry        = {},
            onLoadMore     = {},
            onUserClick    = {}
        )
    }
}