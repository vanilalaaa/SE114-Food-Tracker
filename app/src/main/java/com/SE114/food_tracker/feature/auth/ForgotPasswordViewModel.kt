package com.SE114.food_tracker.feature.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val sent: Boolean = false,
    @StringRes val errorRes: Int? = null
) {
    val canSubmit: Boolean get() = email.isNotBlank() && !isLoading && !sent
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, errorRes = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorRes = null) }
            authRepository.sendPasswordReset(current.email.trim())
                .onSuccess { _state.update { it.copy(isLoading = false, sent = true) } }
                .onFailure {
                    _state.update { it.copy(isLoading = false, errorRes = R.string.auth_forgot_error) }
                }
        }
    }
}
