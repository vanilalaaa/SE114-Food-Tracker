package com.SE114.food_tracker.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.components.AppButton
import com.SE114.food_tracker.core.designsystem.components.AppTextField
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme

@Composable
fun CompleteProfileScreen(
    onComplete: () -> Unit,
    viewModel: CompleteProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.completed) {
        if (state.completed) onComplete()
    }

    val isError = state.userIdStatus == UserIdStatus.Invalid ||
        state.userIdStatus == UserIdStatus.Taken ||
        state.userIdStatus == UserIdStatus.Error
    val helper = when (state.userIdStatus) {
        UserIdStatus.Idle -> stringResource(R.string.auth_complete_user_id_helper)
        UserIdStatus.Checking -> stringResource(R.string.auth_complete_user_id_checking)
        UserIdStatus.Available -> stringResource(R.string.auth_complete_user_id_available)
        else -> null
    }
    val errorText = when (state.userIdStatus) {
        UserIdStatus.Invalid -> stringResource(R.string.auth_complete_user_id_invalid)
        UserIdStatus.Taken -> stringResource(R.string.auth_complete_user_id_taken)
        UserIdStatus.Error -> stringResource(R.string.auth_complete_user_id_error)
        else -> null
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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

            AppTextField(
                value = state.userId,
                onValueChange = viewModel::onUserIdChange,
                label = stringResource(R.string.auth_complete_user_id),
                leadingIcon = Icons.Outlined.AlternateEmail,
                isError = isError,
                errorText = errorText,
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
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSubmit,
                loading = state.isSubmitting
            )
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
private fun CompleteProfileScreenPreview() {
    FoodTrackerTheme {
        CompleteProfileScreen(onComplete = {})
    }
}
