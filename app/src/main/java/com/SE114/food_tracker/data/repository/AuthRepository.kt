package com.SE114.food_tracker.data.repository

import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun currentSessionFlow(): Flow<SessionStatus>
    suspend fun signIn(email: String, password: String): AuthOutcome<Unit>
    suspend fun signUp(email: String, password: String, displayName: String, userId: String): AuthOutcome<Unit>
    suspend fun signInWithGoogle(idToken: String, rawNonce: String): AuthOutcome<Unit>

    /** Sends a recovery email; with the OTP template it delivers a 6-digit code. */
    suspend fun sendPasswordReset(email: String): AuthOutcome<Unit>

    /** Verifies the 6-digit recovery code, yielding a temporary authenticated session. */
    suspend fun verifyRecoveryOtp(email: String, token: String): AuthOutcome<Unit>

    /** Updates the password of the currently (recovery-)authenticated user. */
    suspend fun updatePassword(newPassword: String): AuthOutcome<Unit>

    suspend fun signOut(): AuthOutcome<Unit>
}
