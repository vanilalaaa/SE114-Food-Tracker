package com.SE114.food_tracker.feature.auth

import com.SE114.food_tracker.data.remote.dto.ProfileDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private val auth get() = client.auth

    fun currentSessionFlow(): Flow<SessionStatus> = auth.sessionStatus

    suspend fun signIn(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            }.onFailure { Timber.tag("Auth").e(it, "signIn failed") }
        }

    /**
     * Creates the account, then writes its `profile` row. If the profile insert
     * fails the half-created session is signed out so the caller can retry cleanly,
     * and the failure is surfaced through [Result].
     */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        userId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val uid = auth.currentUserOrNull()?.id
                ?: error("Sign-up did not establish a session")

            try {
                client.postgrest.from("profile").insert(
                    ProfileDTO(id = uid, displayName = displayName, userId = userId, isBanned = false)
                )
            } catch (t: Throwable) {
                runCatching { auth.signOut() }
                throw t
            }
            Unit
        }.onFailure { Timber.tag("Auth").e(it, "signUp failed") }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { auth.resetPasswordForEmail(email) }
                .onFailure { Timber.tag("Auth").e(it, "password reset failed") }
        }

    suspend fun signOut(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { auth.signOut() }
        }

    suspend fun isUserIdAvailable(userId: String): Boolean =
        withContext(Dispatchers.IO) {
            client.postgrest.from("profile")
                .select { filter { eq("user_id", userId) } }
                .decodeList<ProfileDTO>()
                .isEmpty()
        }
}
