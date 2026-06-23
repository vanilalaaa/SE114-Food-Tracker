package com.SE114.food_tracker.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

private const val MIN_PASSWORD_LENGTH = 6

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val done: Boolean = false
) {
    val passwordMismatch: Boolean get() = confirmPassword.isNotEmpty() && confirmPassword != newPassword

    val canSubmit: Boolean
        get() = currentPassword.isNotBlank() &&
            newPassword.length >= MIN_PASSWORD_LENGTH &&
            confirmPassword == newPassword &&
            !isLoading
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    fun onCurrentPasswordChange(value: String) = _state.update { it.copy(currentPassword = value, error = null) }
    fun onNewPasswordChange(value: String) = _state.update { it.copy(newPassword = value, error = null) }
    fun onConfirmPasswordChange(value: String) = _state.update { it.copy(confirmPassword = value, error = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val outcome = authRepository.changePassword(current.currentPassword, current.newPassword)) {
                is AuthOutcome.Success -> _state.update { it.copy(isLoading = false, done = true) }
                is AuthOutcome.Failure -> _state.update { it.copy(isLoading = false, error = outcome.error) }
            }
        }
    }
}
