package com.SE114.food_tracker.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val client: SupabaseClient
) : AuthRepository {

    private val auth get() = client.auth

    override fun currentSessionFlow(): Flow<SessionStatus> = auth.sessionStatus

    override fun hasSession(): Boolean = auth.currentUserOrNull() != null

    override suspend fun awaitActiveSession(): Boolean =
        withTimeoutOrNull(SESSION_SETTLE_TIMEOUT_MS) {
            auth.sessionStatus.first { it is SessionStatus.Authenticated }
        } != null

    override suspend fun signIn(email: String, password: String): AuthOutcome<Unit> = runAuth {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        userId: String
    ): AuthOutcome<Unit> =
        runAuth {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                // The on_auth_user_created trigger reads full_name; user_id is carried too
                // but the chosen handle is committed by the immediate completeOnboarding call.
                data = buildJsonObject {
                    put("full_name", displayName)
                    put("user_id", userId)
                }
            }
            Unit
        }

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String): AuthOutcome<Unit> = runAuth {
        auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
            nonce = rawNonce
        }
    }

    override suspend fun sendPasswordReset(email: String): AuthOutcome<Unit> = runAuth {
        auth.resetPasswordForEmail(email)
    }

    override suspend fun verifyRecoveryOtp(email: String, token: String): AuthOutcome<Unit> = runAuth {
        auth.verifyEmailOtp(type = OtpType.Email.RECOVERY, email = email, token = token)
    }

    override suspend fun verifySignupOtp(email: String, token: String): AuthOutcome<Unit> = runAuth {
        auth.verifyEmailOtp(type = OtpType.Email.SIGNUP, email = email, token = token)
    }

    override suspend fun resendSignupOtp(email: String): AuthOutcome<Unit> = runAuth {
        auth.resendEmail(type = OtpType.Email.SIGNUP, email = email)
    }

    override suspend fun updatePassword(newPassword: String): AuthOutcome<Unit> = runAuth {
        auth.updateUser { password = newPassword }
        Unit
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): AuthOutcome<Unit> = runAuth {
        val email = auth.currentUserOrNull()?.email
            ?: error("No email on the current session")
        // Verify the current password by re-authenticating (config: require current password when
        // updating). Wrong password throws → mapped to InvalidCredentials.
        auth.signInWith(Email) {
            this.email = email
            this.password = currentPassword
        }
        auth.updateUser { password = newPassword }
        Unit
    }

    override suspend fun signOut(): AuthOutcome<Unit> = runAuth {
        auth.signOut()
    }

    private suspend fun <T> runAuth(block: suspend () -> T): AuthOutcome<T> =
        withContext(Dispatchers.IO) {
            runCatching { block() }.fold(
                onSuccess = { AuthOutcome.Success(it) },
                onFailure = { AuthOutcome.Failure(it.toAuthError()) }
            )
        }

    private companion object {
        const val SESSION_SETTLE_TIMEOUT_MS = 3000L
    }
}
