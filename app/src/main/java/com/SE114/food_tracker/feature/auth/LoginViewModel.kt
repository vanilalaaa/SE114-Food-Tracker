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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val authenticated: Boolean = false
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank() && !isLoading
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val outcome = authRepository.signIn(current.email.trim(), current.password)) {
                is AuthOutcome.Success -> {
                    syncManager.startInitialSync()
                    _state.update { it.copy(isLoading = false, authenticated = true) }
                }
                is AuthOutcome.Failure ->
                    _state.update { it.copy(isLoading = false, error = outcome.error) }
            }
        }
    }

    fun signInWithGoogle(idToken: String, rawNonce: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val outcome = authRepository.signInWithGoogle(idToken, rawNonce)) {
                is AuthOutcome.Success -> {
                    syncManager.startInitialSync()
                    _state.update { it.copy(isLoading = false, authenticated = true) }
                }
                is AuthOutcome.Failure ->
                    _state.update { it.copy(isLoading = false, error = outcome.error) }
            }
        }
    }

    fun onGoogleError() = _state.update { it.copy(isLoading = false, error = AuthError.Unknown(null)) }
}
