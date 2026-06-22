package com.SE114.food_tracker.data.repository

import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun currentSessionFlow(): Flow<SessionStatus>
    suspend fun signIn(email: String, password: String): AuthOutcome<Unit>
    suspend fun signUp(email: String, password: String, displayName: String, userId: String): AuthOutcome<Unit>
    suspend fun signInWithGoogle(idToken: String, rawNonce: String): AuthOutcome<Unit>
    suspend fun sendPasswordReset(email: String): AuthOutcome<Unit>
    suspend fun signOut(): AuthOutcome<Unit>
}
