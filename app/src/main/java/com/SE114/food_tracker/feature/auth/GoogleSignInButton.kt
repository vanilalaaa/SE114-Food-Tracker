package com.SE114.food_tracker.feature.auth

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.components.AppButton
import com.SE114.food_tracker.core.designsystem.components.AppButtonVariant
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * "Sign in with Google" button. Owns the Activity and the Credential Manager call
 * (the ViewModel never sees the Activity); a cancelled flow is silent.
 */
@Composable
fun GoogleSignInButton(
    enabled: Boolean,
    onIdToken: (idToken: String, rawNonce: String) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val helper = remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, GoogleSignInHelperEntryPoint::class.java)
            .googleSignInHelper()
    }
    val scope = rememberCoroutineScope()

    AppButton(
        text = stringResource(R.string.auth_google_signin),
        onClick = onClick@{
            val current = activity ?: run { onError(); return@onClick }
            val rawNonce = GoogleSignInHelper.newRawNonce()
            scope.launch {
                try {
                    helper.getIdToken(current, rawNonce)
                        .onSuccess { idToken -> onIdToken(idToken, rawNonce) }
                        .onFailure { e ->
                            if (e !is GoogleSignInException.Cancelled) {
                                Timber.tag("Auth").e(e, "google sign-in failed")
                                onError()
                            }
                        }
                } catch (t: Throwable) {
                    Timber.tag("Auth").e(t, "google sign-in failed")
                    onError()
                }
            }
        },
        variant = AppButtonVariant.Secondary,
        enabled = enabled,
        modifier = modifier
    )
}
