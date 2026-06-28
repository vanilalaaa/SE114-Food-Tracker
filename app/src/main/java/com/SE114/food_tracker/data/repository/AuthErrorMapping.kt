package com.SE114.food_tracker.data.repository

import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.exceptions.RestException
import timber.log.Timber
import java.io.IOException

/**
 * Maps a thrown [Throwable] to a domain [AuthError]. Structured Supabase
 * [AuthErrorCode]s win; message matching is only a fallback for unknown codes.
 * The raw exception is always logged and never surfaced to the user.
 */
internal fun Throwable.toAuthError(): AuthError {
    Timber.tag("Auth").e(this, "auth call failed")

    if (this is AuthWeakPasswordException) return AuthError.WeakPassword

    val code = (this as? AuthRestException)?.errorCode
    if (code != null) {
        return when (code) {
            AuthErrorCode.InvalidCredentials -> AuthError.InvalidCredentials
            AuthErrorCode.EmailNotConfirmed -> AuthError.EmailNotConfirmed
            AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists -> AuthError.EmailAlreadyRegistered
            AuthErrorCode.WeakPassword -> AuthError.WeakPassword
            AuthErrorCode.ValidationFailed -> AuthError.InvalidEmail
            AuthErrorCode.OverRequestRateLimit,
            AuthErrorCode.OverEmailSendRateLimit,
            AuthErrorCode.OverSmsSendRateLimit -> AuthError.RateLimited
            AuthErrorCode.OtpExpired, AuthErrorCode.OtpDisabled -> AuthError.OtpInvalid
            else -> messageFallback()
        }
    }

    if (this is IOException) return AuthError.NoNetwork
    return messageFallback()
}

/**
 * Detects the Postgres unique-violation (SQLSTATE 23505) raised by the
 * case-insensitive `user_id` index and maps it to [AuthError.UserIdTaken];
 * everything else delegates to [toAuthError].
 */
internal fun Throwable.toProfileError(): AuthError {
    val body = buildString {
        append(message.orEmpty())
        if (this@toProfileError is RestException) {
            append(' '); append(error)
            append(' '); append(description)
        }
    }
    if ("23505" in body || "duplicate key" in body || "profile_user_id_unique" in body) {
        Timber.tag("Auth").e(this, "user_id unique violation")
        return AuthError.UserIdTaken
    }
    if ("user_id_cooldown_active" in body) {
        Timber.tag("Auth").e(this, "user_id change cooldown active")
        return AuthError.UserIdChangeCooldown
    }
    return toAuthError()
}

/**
 * Maps admin-RPC failures to domain errors.
 *
 * Updated codes for migration 0008:
 * - `not_authorized`              → [AuthError.NotAuthorized]
 * - `self_action`                 → [AuthError.SelfAction]
 * - `target_is_admin`             → [AuthError.TargetIsAdmin]
 * - `deletion_expired`            → [AuthError.DeletionExpired]
 * - `banned_user_cannot_be_admin` → [AuthError.BannedUserCannotBeAdmin] (CẬP NHẬT MỚI)
 */
internal fun Throwable.toAdminError(): AuthError {
    val body = buildString {
        append(message.orEmpty())
        if (this@toAdminError is RestException) {
            append(' '); append(error)
            append(' '); append(description)
        }
    }

    when {
        "not_authorized" in body -> {
            Timber.tag("Admin").e(this, "admin action rejected: not_authorized")
            return AuthError.NotAuthorized
        }
        "self_action" in body -> {
            Timber.tag("Admin").e(this, "admin action rejected: self_action")
            return AuthError.SelfAction
        }
        "target_is_admin" in body -> {
            Timber.tag("Admin").e(this, "admin action rejected: target_is_admin")
            return AuthError.TargetIsAdmin
        }
        "deletion_expired" in body -> {
            Timber.tag("Admin").e(this, "admin action rejected: deletion_expired")
            return AuthError.DeletionExpired
        }
        "banned_user_cannot_be_admin" in body -> { // CẬP NHẬT MỚI
            Timber.tag("Admin").e(this, "admin action rejected: banned_user_cannot_be_admin")
            return AuthError.BannedUserCannotBeAdmin
        }
    }

    Timber.tag("Admin").e(this, "admin RPC failed")
    val mapped = toAuthError()
    return if (mapped is AuthError.Unknown && this is RestException) {
        AuthError.Unknown(body.trim().ifBlank { null })
    } else {
        mapped
    }
}

private fun Throwable.messageFallback(): AuthError {
    val msg = message?.lowercase().orEmpty()
    return when {
        "unable to resolve host" in msg || "failed to connect" in msg ||
                "network" in msg || "timeout" in msg || "timed out" in msg -> AuthError.NoNetwork
        "invalid login credentials" in msg || "invalid credentials" in msg -> AuthError.InvalidCredentials
        "email not confirmed" in msg -> AuthError.EmailNotConfirmed
        "already registered" in msg || "already been registered" in msg ||
                "user already exists" in msg -> AuthError.EmailAlreadyRegistered
        "weak password" in msg || "password should be" in msg -> AuthError.WeakPassword
        "invalid email" in msg || "unable to validate email" in msg -> AuthError.InvalidEmail
        "rate limit" in msg || "too many requests" in msg -> AuthError.RateLimited
        "otp" in msg || "token has expired" in msg || "invalid token" in msg ||
                "expired or is invalid" in msg -> AuthError.OtpInvalid
        else -> AuthError.Unknown(message)
    }
}