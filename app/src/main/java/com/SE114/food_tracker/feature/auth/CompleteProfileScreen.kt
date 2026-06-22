package com.SE114.food_tracker.feature.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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

@Composable
fun CompleteProfileScreen(
    onComplete: () -> Unit,
    viewModel: CompleteProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.completed) {
        if (state.completed) onComplete()
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) viewModel.onAvatarPicked(bytes)
        }
    }

    CompleteProfileContent(
        state = state,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onUserIdChange = viewModel::onUserIdChange,
        onPickAvatar = {
            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onSubmit = viewModel::submit
    )
}

@Composable
private fun CompleteProfileContent(
    state: CompleteProfileUiState,
    onDisplayNameChange: (String) -> Unit,
    onUserIdChange: (String) -> Unit,
    onPickAvatar: () -> Unit,
    onSubmit: () -> Unit
) {
    val isUserIdError = state.userIdStatus == UserIdStatus.Invalid ||
        state.userIdStatus == UserIdStatus.Taken ||
        state.userIdStatus == UserIdStatus.Error
    val helper = when (state.userIdStatus) {
        UserIdStatus.Idle -> stringResource(R.string.auth_complete_user_id_helper)
        UserIdStatus.Checking -> stringResource(R.string.auth_complete_user_id_checking)
        UserIdStatus.Available -> stringResource(R.string.auth_complete_user_id_available)
        else -> null
    }
    val userIdErrorText = when (state.userIdStatus) {
        UserIdStatus.Invalid -> stringResource(R.string.auth_complete_user_id_invalid)
        UserIdStatus.Taken -> stringResource(R.string.auth_complete_user_id_taken)
        UserIdStatus.Error -> stringResource(R.string.auth_complete_user_id_error)
        else -> null
    }

    AppScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.auth_complete_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = stringResource(R.string.auth_complete_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            EditableAvatar(
                avatarUrl = state.avatarUrl,
                uploading = state.isUploadingAvatar,
                onClick = onPickAvatar,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            AppTextField(
                value = state.displayName,
                onValueChange = onDisplayNameChange,
                label = stringResource(R.string.auth_complete_display_name),
                leadingIcon = Icons.Outlined.Person,
                isError = !state.displayNameValid,
                errorText = stringResource(R.string.auth_complete_display_name_required)
            )

            AppTextField(
                value = state.userId,
                onValueChange = onUserIdChange,
                label = stringResource(R.string.auth_complete_user_id),
                leadingIcon = Icons.Outlined.AlternateEmail,
                isError = isUserIdError,
                errorText = userIdErrorText,
                supportingText = helper,
                trailing = { UserIdStatusIcon(state.userIdStatus) }
            )

            state.error?.let { error ->
                Text(
                    text = error.asMessage(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            AppButton(
                text = stringResource(R.string.auth_complete_submit),
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSubmit,
                loading = state.isSubmitting
            )
        }
    }
}

@Composable
private fun EditableAvatar(
    avatarUrl: String?,
    uploading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(96.dp).clip(CircleShape).clickable(onClick = onClick)) {
        if (avatarUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier.fillMaxSize().background(HintGrayStat),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = CardWhite, modifier = Modifier.size(48.dp))
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(avatarUrl).crossfade(true).build(),
                contentDescription = stringResource(R.string.auth_complete_avatar_desc),
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
        } else {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomEnd).size(28.dp)
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.auth_complete_change_avatar),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}

@Composable
private fun UserIdStatusIcon(status: UserIdStatus) {
    when (status) {
        UserIdStatus.Checking -> CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp
        )
        UserIdStatus.Available -> Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = stringResource(R.string.auth_complete_user_id_available_desc),
            tint = MaterialTheme.colorScheme.primary
        )
        UserIdStatus.Taken, UserIdStatus.Invalid, UserIdStatus.Error -> Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.auth_complete_user_id_taken_desc),
            tint = MaterialTheme.colorScheme.error
        )
        UserIdStatus.Idle -> Unit
    }
}

@Preview(showBackground = true)
@Composable
private fun CompleteProfileContentPreview() {
    FoodTrackerTheme {
        CompleteProfileContent(
            state = CompleteProfileUiState(
                displayName = "An Nguyễn",
                userId = "an.nguyen",
                userIdStatus = UserIdStatus.Available
            ),
            onDisplayNameChange = {},
            onUserIdChange = {},
            onPickAvatar = {},
            onSubmit = {}
        )
    }
}
