package com.SE114.food_tracker.feature.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.navigation.AppDestinations
import com.SE114.food_tracker.core.sync.SyncManager
import com.SE114.food_tracker.data.repository.AuthError
import com.SE114.food_tracker.data.repository.AuthOutcome
import com.SE114.food_tracker.data.repository.AuthRepository
import com.SE114.food_tracker.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val navTarget: PostAuthDestination? = null,
    // Arrived here because the account was banned/soft-deleted and signed out.
    val accountBlocked: Boolean = false
) {
    val canSubmit: Boolean
        get() = identifier.isNotBlank() && password.isNotBlank() && !isLoading
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val postAuthNavigator: PostAuthNavigator,
    private val syncManager: SyncManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(
        LoginUiState(
            accountBlocked = savedStateHandle.get<String>(AppDestinations.Login.ARG_REASON) != null
        )
    )
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onIdentifierChange(value: String) =
        _state.update { it.copy(identifier = value, error = null, accountBlocked = false) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val identifier = current.identifier.trim()

            // An '@' means email; otherwise it's a user_id we must resolve to an email first.
            val email = if (identifier.contains('@')) {
                identifier
            } else {
                when (val lookup = profileRepository.getEmailByUserId(identifier)) {
                    is AuthOutcome.Success -> lookup.data
                    is AuthOutcome.Failure -> {
                        _state.update { it.copy(isLoading = false, error = lookup.error) }
                        return@launch
                    }
                }
            }

            // Unknown user_id → generic InvalidCredentials (never reveal whether a handle exists).
            if (email == null) {
                _state.update { it.copy(isLoading = false, error = AuthError.InvalidCredentials) }
                return@launch
            }

            when (val outcome = authRepository.signIn(email, current.password)) {
                is AuthOutcome.Success -> resolveDestination()
                is AuthOutcome.Failure -> _state.update { it.copy(isLoading = false, error = outcome.error) }
            }
        }
    }

    fun signInWithGoogle(idToken: String, rawNonce: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val outcome = authRepository.signInWithGoogle(idToken, rawNonce)) {
                is AuthOutcome.Success -> resolveDestination()
                is AuthOutcome.Failure -> _state.update { it.copy(isLoading = false, error = outcome.error) }
            }
        }
    }

    fun onGoogleError() = _state.update { it.copy(isLoading = false, error = AuthError.Unknown(null)) }

    private suspend fun resolveDestination() {
        syncManager.startInitialSync()
        when (val result = postAuthNavigator.resolve()) {
            is AuthOutcome.Success -> _state.update { it.copy(isLoading = false, navTarget = result.data) }
            is AuthOutcome.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
        }
    }
}
