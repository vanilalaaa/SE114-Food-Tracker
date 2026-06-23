package com.SE114.food_tracker.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.SE114.food_tracker.core.designsystem.components.AppButtonVariant
import com.SE114.food_tracker.core.designsystem.components.AppScaffold
import com.SE114.food_tracker.core.designsystem.components.AppTextField
import com.SE114.food_tracker.core.designsystem.components.OtpInput
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ForgotPasswordContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onCodeChange = viewModel::onCodeChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onRequestCode = viewModel::requestCode,
        onVerifyCode = viewModel::verifyCode,
        onResendCode = viewModel::resendCode,
        onSetNewPassword = viewModel::setNewPassword,
        onBack = onBack
    )
}

@Composable
private fun ForgotPasswordContent(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRequestCode: () -> Unit,
    onVerifyCode: () -> Unit,
    onResendCode: () -> Unit,
    onSetNewPassword: () -> Unit,
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
            Text(
                text = stringResource(R.string.auth_forgot_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 24.dp)
            )

            when (state.step) {
                ResetStep.Request -> RequestStep(state, onEmailChange, onRequestCode)
                ResetStep.Verify -> VerifyStep(state, onCodeChange, onVerifyCode, onResendCode)
                ResetStep.NewPassword -> NewPasswordStep(
                    state, onPasswordChange, onConfirmPasswordChange, onSetNewPassword
                )
                ResetStep.Done -> DoneStep(onBack)
            }

            state.error?.let { error ->
                Text(
                    text = error.asMessage(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (state.step != ResetStep.Done) {
                TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.auth_forgot_back))
                }
            }
        }
    }
}

@Composable
private fun RequestStep(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onRequestCode: () -> Unit
) {
    Text(
        text = stringResource(R.string.auth_reset_request_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    AppTextField(
        value = state.email,
        onValueChange = onEmailChange,
        label = stringResource(R.string.auth_forgot_email),
        leadingIcon = Icons.Outlined.Email,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        isError = state.error != null
    )
    AppButton(
        text = stringResource(R.string.auth_reset_send_code),
        onClick = onRequestCode,
        modifier = Modifier.fillMaxWidth(),
        enabled = state.canRequest,
        loading = state.isLoading
    )
}

@Composable
private fun VerifyStep(
    state: ForgotPasswordUiState,
    onCodeChange: (String) -> Unit,
    onVerifyCode: () -> Unit,
    onResendCode: () -> Unit
) {
    Text(
        text = stringResource(R.string.auth_reset_verify_subtitle, state.email),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        OtpInput(
            value = state.code,
            onValueChange = onCodeChange,
            isError = state.error != null
        )
    }
    AppButton(
        text = stringResource(R.string.auth_reset_verify_submit),
        onClick = onVerifyCode,
        modifier = Modifier.fillMaxWidth(),
        enabled = state.canVerify,
        loading = state.isLoading
    )
    AppButton(
        text = if (state.resendCooldownSeconds > 0) {
            stringResource(R.string.auth_reset_resend_cooldown, state.resendCooldownSeconds)
        } else {
            stringResource(R.string.auth_reset_resend)
        },
        onClick = onResendCode,
        modifier = Modifier.fillMaxWidth(),
        variant = AppButtonVariant.Text,
        enabled = state.canResend
    )
}

@Composable
private fun NewPasswordStep(
    state: ForgotPasswordUiState,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSetNewPassword: () -> Unit
) {
    Text(
        text = stringResource(R.string.auth_reset_new_password_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    AppTextField(
        value = state.password,
        onValueChange = onPasswordChange,
        label = stringResource(R.string.auth_reset_new_password_label),
        leadingIcon = Icons.Outlined.Lock,
        isPassword = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        supportingText = stringResource(R.string.auth_password_hint)
    )
    AppTextField(
        value = state.confirmPassword,
        onValueChange = onConfirmPasswordChange,
        label = stringResource(R.string.auth_reset_confirm_password_label),
        leadingIcon = Icons.Outlined.Lock,
        isPassword = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        isError = state.passwordMismatch,
        errorText = stringResource(R.string.auth_err_password_mismatch)
    )
    AppButton(
        text = stringResource(R.string.auth_reset_submit),
        onClick = onSetNewPassword,
        modifier = Modifier.fillMaxWidth(),
        enabled = state.canSetPassword,
        loading = state.isLoading
    )
}

@Composable
private fun DoneStep(onBack: () -> Unit) {
    Text(
        text = stringResource(R.string.auth_reset_done_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = stringResource(R.string.auth_reset_done_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    AppButton(
        text = stringResource(R.string.auth_reset_done_login),
        onClick = onBack,
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordRequestPreview() {
    FoodTrackerTheme {
        ForgotPasswordContent(
            state = ForgotPasswordUiState(email = "an@example.com"),
            onEmailChange = {}, onCodeChange = {}, onPasswordChange = {},
            onConfirmPasswordChange = {}, onRequestCode = {}, onVerifyCode = {},
            onResendCode = {}, onSetNewPassword = {}, onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordVerifyPreview() {
    FoodTrackerTheme {
        ForgotPasswordContent(
            state = ForgotPasswordUiState(
                step = ResetStep.Verify,
                email = "an@example.com",
                code = "123",
                resendCooldownSeconds = 42
            ),
            onEmailChange = {}, onCodeChange = {}, onPasswordChange = {},
            onConfirmPasswordChange = {}, onRequestCode = {}, onVerifyCode = {},
            onResendCode = {}, onSetNewPassword = {}, onBack = {}
        )
    }
}
