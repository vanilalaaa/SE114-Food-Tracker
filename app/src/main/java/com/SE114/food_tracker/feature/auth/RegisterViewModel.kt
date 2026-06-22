package com.SE114.food_tracker.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.sync.SyncManager
import com.SE114.food_tracker.data.repository.AuthError
import com.SE114.food_tracker.data.repository.AuthOutcome
import com.SE114.food_tracker.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val navTarget: PostAuthDestination? = null
) {
    val passwordMismatch: Boolean get() = confirmPassword.isNotEmpty() && confirmPassword != password

    val canSubmit: Boolean
        get() = displayName.isNotBlank() &&
            email.isNotBlank() &&
            password.isNotBlank() &&
            confirmPassword == password &&
            !isLoading
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val postAuthNavigator: PostAuthNavigator,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onDisplayNameChange(value: String) = _state.update { it.copy(displayName = value, error = null) }
    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onConfirmPasswordChange(value: String) = _state.update { it.copy(confirmPassword = value, error = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val outcome = authRepository.signUp(
                email = current.email.trim(),
                password = current.password,
                displayName = current.displayName.trim()
            )
            when (outcome) {
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
