package com.SE114.food_tracker.feature.auth

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.R

/**
 * Shared presentation for the live user_id availability field used by both the
 * Register and Complete-Profile screens. Keeps the icon and the supporting/error
 * copy identical across the two flows.
 */

internal fun UserIdStatus.isError(): Boolean =
    this == UserIdStatus.Invalid || this == UserIdStatus.Taken || this == UserIdStatus.Error

@Composable
internal fun userIdSupportingText(status: UserIdStatus): String? = when (status) {
    UserIdStatus.Idle -> stringResource(R.string.auth_complete_user_id_helper)
    UserIdStatus.Checking -> stringResource(R.string.auth_complete_user_id_checking)
    UserIdStatus.Available -> stringResource(R.string.auth_complete_user_id_available)
    else -> null
}

@Composable
internal fun userIdErrorText(status: UserIdStatus): String? = when (status) {
    UserIdStatus.Invalid -> stringResource(R.string.auth_complete_user_id_invalid)
    UserIdStatus.Taken -> stringResource(R.string.auth_complete_user_id_taken)
    UserIdStatus.Error -> stringResource(R.string.auth_complete_user_id_error)
    else -> null
}

@Composable
internal fun UserIdStatusIcon(status: UserIdStatus) {
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
