package com.SE114.food_tracker.feature.admin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.SE114.food_tracker.R
import com.SE114.food_tracker.data.repository.AuthError

/** Admin-screen message for an [AuthError] (feature-local; cannot import feature/auth). */
@Composable
fun AuthError.adminMessage(): String = when (this) {
    AuthError.NotAuthorized -> stringResource(R.string.admin_err_not_authorized)
    AuthError.NoNetwork -> stringResource(R.string.auth_err_no_network)
    // Surface the underlying server reason (missing RPC / column, etc.) instead of a vague
    // generic message, so admin-data failures are actually diagnosable on-device.
    is AuthError.Unknown -> raw?.takeIf { it.isNotBlank() } ?: stringResource(R.string.admin_err_generic)
    else -> stringResource(R.string.admin_err_generic)
}
