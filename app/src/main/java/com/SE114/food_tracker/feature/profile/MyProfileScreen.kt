package com.SE114.food_tracker.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.components.AppButton
import com.SE114.food_tracker.core.designsystem.components.AppScaffold
import com.SE114.food_tracker.core.designsystem.components.AppTextField
import com.SE114.food_tracker.core.designsystem.theme.CardWhite
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.HintGrayStat
import com.SE114.food_tracker.data.repository.AuthError

@Composable
fun MyProfileScreen(
    onBack: () -> Unit,
    viewModel: MyProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var isEditing by rememberSaveable { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) viewModel.onAvatarPicked(bytes)
        }
    }

    // A successful save returns to the read-only view.
    LaunchedEffect(state.saveSucceeded) {
        if (state.saveSucceeded) {
            isEditing = false
            viewModel.consumeSaveSuccess()
        }
    }

    MyProfileContent(
        state = state,
        isEditing = isEditing,
        onBack = {
            if (isEditing) {
                viewModel.discardEdits()
                isEditing = false
            } else {
                onBack()
            }
        },
        onAvatarTap = {
            if (isEditing) {
                avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                isEditing = true
            }
        },
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onUserIdChange = viewModel::onUserIdChange,
        onSave = viewModel::save
    )
}

@Composable
private fun MyProfileContent(
    state: MyProfileUiState,
    isEditing: Boolean,
    onBack: () -> Unit,
    onAvatarTap: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onUserIdChange: (String) -> Unit,
    onSave: () -> Unit
) {
    AppScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.profile_back)
                    )
                }
                Text(
                    text = stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(Modifier.height(8.dp))

            EditableAvatar(
                avatarUrl = state.avatarUrl,
                uploading = state.isUploadingAvatar,
                showPencil = true,
                size = 120.dp,
                onClick = onAvatarTap,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (!isEditing) {
                Text(
                    text = state.displayName.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.settings_default_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = state.userId.takeIf { it.isNotBlank() }?.let { "@$it" }
                        ?: stringResource(R.string.settings_no_user_id),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                AppTextField(
                    value = state.displayName,
                    onValueChange = onDisplayNameChange,
                    label = stringResource(R.string.profile_display_name),
                    leadingIcon = Icons.Outlined.Person,
                    isError = !state.displayNameValid,
                    errorText = stringResource(R.string.profile_display_name_required)
                )

                UserIdField(state = state, onUserIdChange = onUserIdChange)

                state.error?.let { error ->
                    Text(
                        text = error.profileMessage(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                AppButton(
                    text = stringResource(R.string.profile_save),
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canSave,
                    loading = state.isSaving
                )
            }
        }
    }
}

@Composable
private fun UserIdField(state: MyProfileUiState, onUserIdChange: (String) -> Unit) {
    if (!state.userIdEditable) {
        AppTextField(
            value = state.userId,
            onValueChange = {},
            label = stringResource(R.string.profile_user_id),
            leadingIcon = Icons.Outlined.AlternateEmail,
            enabled = false,
            supportingText = stringResource(R.string.profile_user_id_cooldown, state.cooldownDays)
        )
        return
    }

    val isError = state.userIdStatus == UserIdCheckStatus.Invalid ||
        state.userIdStatus == UserIdCheckStatus.Taken ||
        state.userIdStatus == UserIdCheckStatus.Error
    val helper = when (state.userIdStatus) {
        UserIdCheckStatus.Idle -> stringResource(R.string.profile_user_id_helper)
        UserIdCheckStatus.Checking -> stringResource(R.string.profile_user_id_checking)
        UserIdCheckStatus.Available -> stringResource(R.string.profile_user_id_available)
        else -> null
    }
    val errorText = when (state.userIdStatus) {
        UserIdCheckStatus.Invalid -> stringResource(R.string.profile_user_id_invalid)
        UserIdCheckStatus.Taken -> stringResource(R.string.profile_user_id_taken)
        UserIdCheckStatus.Error -> stringResource(R.string.profile_user_id_check_error)
        else -> null
    }

    AppTextField(
        value = state.userId,
        onValueChange = onUserIdChange,
        label = stringResource(R.string.profile_user_id),
        leadingIcon = Icons.Outlined.AlternateEmail,
        isError = isError,
        errorText = errorText,
        supportingText = helper,
        trailing = { UserIdStatusIcon(state.userIdStatus) }
    )
}

@Composable
private fun UserIdStatusIcon(status: UserIdCheckStatus) {
    when (status) {
        UserIdCheckStatus.Checking -> CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp
        )
        UserIdCheckStatus.Available -> Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = stringResource(R.string.profile_user_id_available),
            tint = MaterialTheme.colorScheme.primary
        )
        UserIdCheckStatus.Taken, UserIdCheckStatus.Invalid, UserIdCheckStatus.Error -> Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.profile_user_id_taken),
            tint = MaterialTheme.colorScheme.error
        )
        UserIdCheckStatus.Idle -> Unit
    }
}

/**
 * Circular avatar with an optional edit-pencil badge. The circular clip is applied only to the
 * image, so the badge sits at the edge and is never clipped by the circle.
 */
@Composable
private fun EditableAvatar(
    avatarUrl: String?,
    uploading: Boolean,
    showPencil: Boolean,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size).clickable(onClick = onClick)) {
        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(HintGrayStat)) {
            if (avatarUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = CardWhite,
                    modifier = Modifier.align(Alignment.Center).size(size / 2)
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(avatarUrl).crossfade(true).build(),
                    contentDescription = stringResource(R.string.profile_avatar_desc),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (uploading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CardWhite, modifier = Modifier.size(28.dp))
                }
            }
        }

        if (showPencil && !uploading) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                border = androidx.compose.foundation.BorderStroke(2.dp, CardWhite),
                modifier = Modifier.align(Alignment.BottomEnd).size(34.dp)
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.profile_change_avatar),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun AuthError.profileMessage(): String = when (this) {
    AuthError.UserIdTaken -> stringResource(R.string.auth_err_user_id_taken)
    AuthError.UserIdChangeCooldown -> stringResource(R.string.auth_err_user_id_cooldown)
    AuthError.NoNetwork -> stringResource(R.string.auth_err_no_network)
    else -> stringResource(R.string.auth_err_unknown)
}

@Preview(showBackground = true, name = "Profile view")
@Composable
private fun MyProfileViewPreview() {
    FoodTrackerTheme {
        MyProfileContent(
            state = MyProfileUiState(
                loading = false,
                displayName = "An Nguyễn",
                userId = "an.nguyen",
                originalUserId = "an.nguyen"
            ),
            isEditing = false,
            onBack = {},
            onAvatarTap = {},
            onDisplayNameChange = {},
            onUserIdChange = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, name = "Profile edit")
@Composable
private fun MyProfileEditPreview() {
    FoodTrackerTheme {
        MyProfileContent(
            state = MyProfileUiState(
                loading = false,
                displayName = "An Nguyễn",
                userId = "an.nguyen",
                originalUserId = "an.nguyen",
                userIdEditable = true
            ),
            isEditing = true,
            onBack = {},
            onAvatarTap = {},
            onDisplayNameChange = {},
            onUserIdChange = {},
            onSave = {}
        )
    }
}
