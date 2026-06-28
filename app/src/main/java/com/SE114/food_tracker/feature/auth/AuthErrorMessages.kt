package com.SE114.food_tracker.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.SE114.food_tracker.R
import com.SE114.food_tracker.data.repository.AuthError

@Composable
fun AuthError.asMessage(): String = when (this) {
    AuthError.InvalidCredentials -> stringResource(R.string.auth_err_invalid_credentials)
    AuthError.EmailNotConfirmed -> stringResource(R.string.auth_err_email_not_confirmed)
    AuthError.EmailAlreadyRegistered -> stringResource(R.string.auth_err_email_registered)
    AuthError.WeakPassword -> stringResource(R.string.auth_err_weak_password)
    AuthError.InvalidEmail -> stringResource(R.string.auth_err_invalid_email)
    AuthError.RateLimited -> stringResource(R.string.auth_err_rate_limited)
    AuthError.UserIdTaken -> stringResource(R.string.auth_err_user_id_taken)
    AuthError.UserIdChangeCooldown -> stringResource(R.string.auth_err_user_id_cooldown)
    AuthError.OtpInvalid -> stringResource(R.string.auth_err_otp_invalid)
    AuthError.NoNetwork -> stringResource(R.string.auth_err_no_network)
    AuthError.NotAuthorized -> stringResource(R.string.admin_err_not_authorized)

    AuthError.SelfAction -> stringResource(R.string.admin_err_self_action)
    AuthError.TargetIsAdmin -> stringResource(R.string.admin_err_target_is_admin)
    AuthError.DeletionExpired -> stringResource(R.string.admin_err_deletion_expired)
    AuthError.BannedUserCannotBeAdmin -> stringResource(R.string.admin_err_banned_user_cannot_be_admin)

    is AuthError.Unknown -> stringResource(R.string.auth_err_unknown)
}