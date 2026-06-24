package com.SE114.food_tracker.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
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
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.data.repository.AuthError

@Composable
fun LoginScreen(
    onAuthenticated: (PostAuthDestination) -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateForgot: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navTarget) {
        state.navTarget?.let(onAuthenticated)
    }

    LoginContent(
        state = state,
        onIdentifierChange = viewModel::onIdentifierChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onNavigateRegister = onNavigateRegister,
        onNavigateForgot = onNavigateForgot,
        googleButton = {
            GoogleSignInButton(
                enabled = !state.isLoading,
                onIdToken = viewModel::signInWithGoogle,
                onError = viewModel::onGoogleError,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    onIdentifierChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateForgot: () -> Unit,
    googleButton: @Composable () -> Unit
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
                text = stringResource(R.string.auth_login_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            if (state.adminMode) {
                Text(
                    text = stringResource(R.string.admin_login_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (state.adminAccessDenied) {
                Text(
                    text = stringResource(R.string.admin_access_denied),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            AppTextField(
                value = state.identifier,
                onValueChange = onIdentifierChange,
                label = stringResource(R.string.auth_login_identifier),
                leadingIcon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                supportingText = stringResource(R.string.auth_login_identifier_helper),
                isError = state.error != null
            )

            AppTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.auth_login_password),
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = state.error != null
            )

            state.error?.let { error ->
                Text(
                    text = error.asMessage(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            AppButton(
                text = stringResource(R.string.auth_login_submit),
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSubmit,
                loading = state.isLoading
            )

            googleButton()

            TextButton(
                onClick = onNavigateForgot,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.auth_login_forgot))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.auth_login_no_account),
                    style = MaterialTheme.typography.bodyLarge
                )
                AppButton(
                    text = stringResource(R.string.auth_login_register_cta),
                    onClick = onNavigateRegister,
                    variant = AppButtonVariant.Text
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContentPreview() {
    FoodTrackerTheme {
        LoginContent(
            state = LoginUiState(identifier = "an@example.com", error = AuthError.InvalidCredentials),
            onIdentifierChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onNavigateRegister = {},
            onNavigateForgot = {},
            googleButton = {
                AppButton(
                    text = stringResource(R.string.auth_google_signin),
                    onClick = {},
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}
