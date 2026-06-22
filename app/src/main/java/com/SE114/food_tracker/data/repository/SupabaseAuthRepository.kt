package com.SE114.food_tracker.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
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
}
