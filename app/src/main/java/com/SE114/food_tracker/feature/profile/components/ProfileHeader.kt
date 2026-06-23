package com.SE114.food_tracker.feature.profile.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.components.LoadingShimmer
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.TextLabelGray
import com.SE114.food_tracker.core.designsystem.theme.TextPrimary
import com.SE114.food_tracker.data.remote.dto.ProfileDTO
import com.SE114.food_tracker.feature.friend.components.ProfileAvatar
import com.SE114.food_tracker.feature.profile.ProfileUiState

private val ProfileAvatarSize = 96.dp

@Composable
fun ProfileHeader(
    uiState: ProfileUiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onReportClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ProfileHeaderTopBar(
            onNavigateBack = onNavigateBack,
            showReportAction = !uiState.isSelf &&
                !uiState.isLoading &&
                uiState.error == null &&
                uiState.profile != null,
            onReportClick = onReportClick
        )

        Spacer(Modifier.height(24.dp))

        when {
            uiState.isLoading -> ProfileHeaderLoadingContent()
            uiState.error != null -> ProfileHeaderErrorContent(
                message = uiState.error,
                onRetry = onRetry
            )
            else -> ProfileHeaderContent(
                displayName = uiState.displayName,
                handle = uiState.handle,
                avatarUrl = uiState.profile?.avatarUrl,
                isSelf = uiState.isSelf
            )
        }
    }
}

@Composable
private fun ProfileHeaderTopBar(
    onNavigateBack: () -> Unit,
    showReportAction: Boolean,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .offset(x = (-12).dp)
            .padding(top = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = stringResource(R.string.profile_back),
                tint = TextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = stringResource(R.string.profile_viewer_title),
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        if (showReportAction) {
            Box {
                IconButton(onClick = { isMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.profile_menu_open),
                        tint = TextPrimary
                    )
                }

                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false },
                    modifier = Modifier.width(104.dp),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = Color.White,
                    shadowElevation = 4.dp
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.profile_report),
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = MenuDefaults.itemColors(textColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        onClick = {
                            isMenuExpanded = false
                            onReportClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderContent(
    displayName: String,
    handle: String,
    avatarUrl: String?,
    isSelf: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProfileAvatar(
            avatarUrl = avatarUrl,
            hasStory = false,
            size = ProfileAvatarSize
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = displayName,
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        if (handle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = handle,
                color = TextLabelGray,
                fontSize = 14.sp
            )
        }

        if (isSelf) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.profile_self_badge),
                color = TextLabelGray,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ProfileHeaderLoadingContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoadingShimmer(
            modifier = Modifier.size(ProfileAvatarSize),
            shape = CircleShape
        )
        Spacer(Modifier.height(16.dp))
        LoadingShimmer(
            modifier = Modifier
                .width(160.dp)
                .height(20.dp)
        )
        Spacer(Modifier.height(8.dp))
        LoadingShimmer(
            modifier = Modifier
                .width(100.dp)
                .height(14.dp)
        )
    }
}

@Composable
private fun ProfileHeaderErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = TextLabelGray,
            fontSize = 14.sp
        )
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.profile_retry))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileHeaderContentPreview() {
    FoodTrackerTheme {
        ProfileHeader(
            uiState = ProfileUiState(
                profile = ProfileDTO(
                    id = "id-1",
                    displayName = "Thảo Uyên",
                    userId = "uyen_123",
                    avatarUrl = null
                ),
                isSelf = false
            ),
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileHeaderLoadingPreview() {
    FoodTrackerTheme {
        ProfileHeader(
            uiState = ProfileUiState(isLoading = true),
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileHeaderErrorPreview() {
    FoodTrackerTheme {
        ProfileHeader(
            uiState = ProfileUiState(
                error = "Không tải được profile."
            ),
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileHeaderSelfPreview() {
    FoodTrackerTheme {
        ProfileHeader(
            uiState = ProfileUiState(
                profile = ProfileDTO(
                    id = "id-1",
                    displayName = "Thảo Uyên",
                    userId = "uyen_123"
                ),
                isSelf = true
            ),
            onNavigateBack = {},
            onRetry = {}
        )
    }
}
