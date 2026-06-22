package com.SE114.food_tracker.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.components.AppButton
import com.SE114.food_tracker.core.designsystem.components.AppButtonVariant
import com.SE114.food_tracker.core.designsystem.components.AppScaffold
import com.SE114.food_tracker.core.designsystem.components.OtpInput
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme

@Composable
fun VerifyEmailScreen(
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: VerifyEmailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.completed) {
        if (state.completed) onVerified()
    }

    VerifyEmailContent(
        email = viewModel.email,
        state = state,
        onCodeChange = viewModel::onCodeChange,
        onVerify = viewModel::verify,
        onResend = viewModel::resend,
        onBack = onBack
    )
}

@Composable
private fun VerifyEmailContent(
    email: String,
    state: VerifyEmailUiState,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
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
                    contentDescription = stringResource(R.string.auth_verify_email_back)
                )
            }

            Text(
                text = stringResource(R.string.auth_verify_email_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.auth_verify_email_subtitle, email),
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

            state.error?.let { error ->
                Text(
                    text = error.asMessage(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            AppButton(
                text = stringResource(R.string.auth_verify_email_submit),
                onClick = onVerify,
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
                onClick = onResend,
                modifier = Modifier.fillMaxWidth(),
                variant = AppButtonVariant.Text,
                enabled = state.canResend
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VerifyEmailContentPreview() {
    FoodTrackerTheme {
        VerifyEmailContent(
            email = "an@example.com",
            state = VerifyEmailUiState(code = "1234", resendCooldownSeconds = 42),
            onCodeChange = {},
            onVerify = {},
            onResend = {},
            onBack = {}
        )
    }
}
