package com.SE114.food_tracker.feature.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.sync.SyncManager
import com.SE114.food_tracker.data.repository.AuthError
import com.SE114.food_tracker.data.repository.AuthOutcome
import com.SE114.food_tracker.data.repository.AuthRepository
import com.SE114.food_tracker.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerifyEmailUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val resendCooldownSeconds: Int = 0,
    val completed: Boolean = false
) {
    val canVerify: Boolean get() = code.length == OTP_LENGTH && !isLoading
    val canResend: Boolean get() = resendCooldownSeconds == 0 && !isLoading
}

@HiltViewModel
class VerifyEmailViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val syncManager: SyncManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Nav path args; Navigation has already URL-decoded them.
    val email: String = savedStateHandle.get<String>("email").orEmpty()
    private val displayName: String = savedStateHandle.get<String>("displayName").orEmpty()
    private val userId: String = savedStateHandle.get<String>("userId").orEmpty()

    private val _state = MutableStateFlow(VerifyEmailUiState())
    val state: StateFlow<VerifyEmailUiState> = _state.asStateFlow()

    private var cooldownJob: Job? = null

    init { startResendCooldown() } // signUp already delivered the first code

    fun onCodeChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(OTP_LENGTH)
        _state.update { it.copy(code = digits, error = null) }
    }

    fun verify() {
        val current = _state.value
        if (!current.canVerify) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val outcome = authRepository.verifySignupOtp(email, current.code)) {
                is AuthOutcome.Success -> finishOnboarding()
                is AuthOutcome.Failure -> _state.update { it.copy(isLoading = false, error = outcome.error) }
            }
        }
    }

    fun resend() {
        if (!_state.value.canResend) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val outcome = authRepository.resendSignupOtp(email)) {
                is AuthOutcome.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    startResendCooldown()
                }
                is AuthOutcome.Failure -> _state.update { it.copy(isLoading = false, error = outcome.error) }
            }
        }
    }

    // Verification yields an active session; fill user_id + onboarding_completed, then enter the app.
    private suspend fun finishOnboarding() {
        when (val outcome = profileRepository.completeOnboarding(displayName, userId, avatarUrl = null)) {
            is AuthOutcome.Success -> {
                syncManager.startInitialSync()
                _state.update { it.copy(isLoading = false, completed = true) }
            }
            is AuthOutcome.Failure -> _state.update { it.copy(isLoading = false, error = outcome.error) }
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
        private const val RESEND_COOLDOWN_SECONDS = 60
    }
}
