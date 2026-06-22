package com.SE114.food_tracker.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.data.repository.AuthError
import com.SE114.food_tracker.data.repository.AuthOutcome
import com.SE114.food_tracker.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val OTP_LENGTH = 6

enum class ResetStep { Request, Verify, NewPassword, Done }

data class ForgotPasswordUiState(
    val step: ResetStep = ResetStep.Request,
    val email: String = "",
    val code: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val resendCooldownSeconds: Int = 0
) {
    val passwordMismatch: Boolean get() = confirmPassword.isNotEmpty() && confirmPassword != password

    val canRequest: Boolean get() = email.isNotBlank() && !isLoading
    val canVerify: Boolean get() = code.length == OTP_LENGTH && !isLoading
    val canResend: Boolean get() = resendCooldownSeconds == 0 && !isLoading
    val canSetPassword: Boolean
        get() = password.length >= MIN_PASSWORD_LENGTH && confirmPassword == password && !isLoading
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    private var cooldownJob: Job? = null

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }

    fun onCodeChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(OTP_LENGTH)
        _state.update { it.copy(code = digits, error = null) }
    }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onConfirmPasswordChange(value: String) = _state.update { it.copy(confirmPassword = value, error = null) }

    fun requestCode() {
        if (!_state.value.canRequest) return
        sendCode(advanceToVerify = true)
    }

    fun resendCode() {
        if (!_state.value.canResend) return
        sendCode(advanceToVerify = false)
    }

    fun verifyCode() {
        val current = _state.value
        if (!current.canVerify) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val outcome = authRepository.verifyRecoveryOtp(current.email.trim(), current.code)) {
                is AuthOutcome.Success -> _state.update { it.copy(isLoading = false, step = ResetStep.NewPassword) }
                is AuthOutcome.Failure -> _state.update { it.copy(isLoading = false, error = outcome.error) }
            }
        }
    }

    fun setNewPassword() {
        val current = _state.value
        if (!current.canSetPassword) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val outcome = authRepository.updatePassword(current.password)) {
                is AuthOutcome.Success -> {
                    // Drop the temporary recovery session so the user signs in fresh.
                    authRepository.signOut()
                    _state.update { it.copy(isLoading = false, step = ResetStep.Done) }
                }
                is AuthOutcome.Failure -> _state.update { it.copy(isLoading = false, error = outcome.error) }
            }
        }
    }

    private fun sendCode(advanceToVerify: Boolean) {
        val email = _state.value.email.trim()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val outcome = authRepository.sendPasswordReset(email)) {
                is AuthOutcome.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            step = if (advanceToVerify) ResetStep.Verify else it.step
                        )
                    }
                    startResendCooldown()
                }
                is AuthOutcome.Failure -> _state.update { it.copy(isLoading = false, error = outcome.error) }
            }
        }
    }

    private fun startResendCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _state.update { it.copy(resendCooldownSeconds = RESEND_COOLDOWN_SECONDS) }
            while (_state.value.resendCooldownSeconds > 0) {
                delay(1_000)
                _state.update { it.copy(resendCooldownSeconds = it.resendCooldownSeconds - 1) }
            }
        }
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
        private const val RESEND_COOLDOWN_SECONDS = 60
    }
}
