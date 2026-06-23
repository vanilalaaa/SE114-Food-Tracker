package com.SE114.food_tracker.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.components.AppButton
import com.SE114.food_tracker.core.designsystem.components.AppScaffold
import com.SE114.food_tracker.core.designsystem.components.AppTextField
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.data.repository.AuthError

@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ChangePasswordContent(
        state = state,
        onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onSubmit = viewModel::submit,
        onBack = onBack
    )
}

@Composable
private fun ChangePasswordContent(
    state: ChangePasswordUiState,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    AppScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.auth_change_pw_back)
                )
            }

            Text(
                text = stringResource(R.string.auth_change_pw_title),
                style = MaterialTheme.typography.titleLarge
            )

            if (state.done) {
                SuccessState(onBack)
                return@Column
            }

            AppTextField(
                value = state.currentPassword,
                onValueChange = onCurrentPasswordChange,
                label = stringResource(R.string.auth_change_pw_current),
                leadingIcon = Icons.Outlined.LockReset,
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = state.error == AuthError.InvalidCredentials,
                errorText = stringResource(R.string.auth_change_pw_wrong_current)
            )

            AppTextField(
                value = state.newPassword,
                onValueChange = onNewPasswordChange,
                label = stringResource(R.string.auth_change_pw_new),
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = stringResource(R.string.auth_password_hint)
            )

            AppTextField(
                value = state.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = stringResource(R.string.auth_change_pw_confirm),
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = state.passwordMismatch,
                errorText = stringResource(R.string.auth_err_password_mismatch)
            )

            // The current-password error renders on its field; show other errors here.
            state.error?.takeIf { it != AuthError.InvalidCredentials }?.let { error ->
                Text(
                    text = error.asMessage(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            AppButton(
                text = stringResource(R.string.auth_change_pw_submit),
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSubmit,
                loading = state.isLoading
            )
        }
    }
}

@Composable
private fun SuccessState(onBack: () -> Unit) {
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(48.dp)
    )
    Text(
        text = stringResource(R.string.auth_change_pw_success),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary
    )
    AppButton(
        text = stringResource(R.string.auth_change_pw_back),
        onClick = onBack,
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
private fun ChangePasswordContentPreview() {
    FoodTrackerTheme {
        ChangePasswordContent(
            state = ChangePasswordUiState(currentPassword = "old", newPassword = "newpass1", confirmPassword = "newpass1"),
            onCurrentPasswordChange = {},
            onNewPasswordChange = {},
            onConfirmPasswordChange = {},
            onSubmit = {},
            onBack = {}
        )
    }
}
