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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.SE114.food_tracker.core.designsystem.components.AppTextField
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.registered) {
        if (state.registered) onRegisterSuccess()
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
                text = stringResource(R.string.auth_register_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            AppTextField(
                value = state.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                label = stringResource(R.string.auth_register_display_name),
                leadingIcon = Icons.Outlined.Person
            )

            AppTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = stringResource(R.string.auth_register_email),
                leadingIcon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            AppTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = stringResource(R.string.auth_register_password),
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            AppTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = stringResource(R.string.auth_register_confirm_password),
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = state.passwordMismatch,
                errorText = stringResource(R.string.auth_err_password_mismatch)
            )

            state.error?.let { error ->
                Text(
                    text = error.asMessage(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            AppButton(
                text = stringResource(R.string.auth_register_submit),
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSubmit,
                loading = state.isLoading
            )

            GoogleSignInButton(
                enabled = !state.isLoading,
                onIdToken = viewModel::signInWithGoogle,
                onError = viewModel::onGoogleError,
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.auth_register_have_account),
                    style = MaterialTheme.typography.bodyLarge
                )
                AppButton(
                    text = stringResource(R.string.auth_register_login_cta),
                    onClick = onNavigateLogin,
                    variant = AppButtonVariant.Text
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    FoodTrackerTheme {
        RegisterScreen(onRegisterSuccess = {}, onNavigateLogin = {})
    }
}
