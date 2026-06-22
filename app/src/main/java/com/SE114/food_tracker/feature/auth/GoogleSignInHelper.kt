package com.SE114.food_tracker.feature.auth

import android.app.Activity
import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.SE114.food_tracker.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

sealed class GoogleSignInException(message: String) : Exception(message) {
    data object Cancelled : GoogleSignInException("Google sign-in cancelled")
    data object NoCredential : GoogleSignInException("No Google credential available")
}

/**
 * Owns the Credential Manager "Sign in with Google" flow. The screen passes the
 * Activity (Credential Manager needs an Activity to show its UI); the raw nonce
 * is generated per request, hashed for Google, and returned to the caller so it
 * can be forwarded to Supabase unhashed.
 */
@Singleton
class GoogleSignInHelper @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    suspend fun getIdToken(activity: Activity, rawNonce: String): Result<String> {
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setNonce(sha256Hex(rawNonce))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = CredentialManager.create(appContext).getCredential(activity, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                Result.success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Result.failure(GoogleSignInException.NoCredential)
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(GoogleSignInException.Cancelled)
        } catch (e: NoCredentialException) {
            Result.failure(GoogleSignInException.NoCredential)
        }
    }

    private fun sha256Hex(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        fun newRawNonce(): String {
            val bytes = ByteArray(NONCE_BYTES).also { SecureRandom().nextBytes(it) }
            return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }

        private const val NONCE_BYTES = 32
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GoogleSignInHelperEntryPoint {
    fun googleSignInHelper(): GoogleSignInHelper
}
