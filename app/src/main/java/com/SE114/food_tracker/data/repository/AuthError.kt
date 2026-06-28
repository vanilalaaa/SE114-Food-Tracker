package com.SE114.food_tracker.data.repository

/**
 * Domain-level auth/profile failure. Pure data — no Android/UI references. The UI
 * layer maps each case to a localized message (see `feature/auth/AuthErrorMessages`).
 */
sealed interface AuthError {
    data object InvalidCredentials : AuthError
    data object EmailNotConfirmed : AuthError
    data object EmailAlreadyRegistered : AuthError
    data object WeakPassword : AuthError
    data object InvalidEmail : AuthError
    data object RateLimited : AuthError
    data object UserIdTaken : AuthError
    data object UserIdChangeCooldown : AuthError
    data object OtpInvalid : AuthError
    data object NoNetwork : AuthError
    data object NotAuthorized : AuthError
    data object SelfAction : AuthError
    data object TargetIsAdmin : AuthError
    data object DeletionExpired : AuthError
    data object BannedUserCannotBeAdmin : AuthError
    data class Unknown(val raw: String?) : AuthError
}