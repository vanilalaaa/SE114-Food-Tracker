package com.SE114.food_tracker.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.sync.SyncManager
import com.SE114.food_tracker.data.repository.AuthError
import com.SE114.food_tracker.data.repository.AuthOutcome
import com.SE114.food_tracker.data.repository.AuthRepository
import com.SE114.food_tracker.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val displayName: String = "",
    val userId: String = "",
    val userIdStatus: UserIdStatus = UserIdStatus.Idle,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val navTarget: PostAuthDestination? = null,
    val pendingVerification: PendingVerification? = null
) {
    val passwordMismatch: Boolean get() = confirmPassword.isNotEmpty() && confirmPassword != password

    // Available => clear to submit. Error => the availability probe failed (e.g. offline);
    // still allow submit and let the DB unique index be the authority. Everything else blocks.
    val userIdReady: Boolean
        get() = userIdStatus == UserIdStatus.Available || userIdStatus == UserIdStatus.Error

    val canSubmit: Boolean
        get() = displayName.isNotBlank() &&
            userIdReady &&
            email.isNotBlank() &&
            password.isNotBlank() &&
            confirmPassword == password &&
            !isLoading
}

/** Carries the typed-email signup details to the Verify-Email screen (no session exists yet). */
data class PendingVerification(
    val email: String,
    val displayName: String,
    val userId: String
)

@OptIn(FlowPreview::class)
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val postAuthNavigator: PostAuthNavigator,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    private val userIdInput = MutableStateFlow("")

    init {
        viewModelScope.launch {
            userIdInput
                .debounce(USER_ID_DEBOUNCE_MS)
                .collectLatest { id ->
                    when {
                        id.isEmpty() -> setUserIdStatus(UserIdStatus.Idle)
                        !CompleteProfileViewModel.USER_ID_REGEX.matches(id) ->
                            setUserIdStatus(UserIdStatus.Invalid)
                        else -> {
                            setUserIdStatus(UserIdStatus.Checking)
                            val status = when (val outcome = profileRepository.isUserIdAvailable(id)) {
                                is AuthOutcome.Success ->
                                    if (outcome.data) UserIdStatus.Available else UserIdStatus.Taken
                                is AuthOutcome.Failure -> UserIdStatus.Error
                            }
                            setUserIdStatus(status)
                        }
                    }
                }
        }
    }

    fun onDisplayNameChange(value: String) = _state.update { it.copy(displayName = value, error = null) }
    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onConfirmPasswordChange(value: String) = _state.update { it.copy(confirmPassword = value, error = null) }

    fun onUserIdChange(value: String) {
        _state.update {
            it.copy(
                userId = value,
                userIdStatus = if (value.isEmpty()) UserIdStatus.Idle else UserIdStatus.Checking,
                error = null
            )
        }
        userIdInput.value = value
    }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val signUp = authRepository.signUp(
                email = current.email.trim(),
                password = current.password,
                displayName = current.displayName.trim(),
                userId = current.userId.trim()
            )
            when (signUp) {
                is AuthOutcome.Failure -> _state.update { it.copy(isLoading = false, error = signUp.error) }
                // Email confirmation is ON: signUp returns no session — go verify the OTP first.
                is AuthOutcome.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        pendingVerification = PendingVerification(
                            email = current.email.trim(),
                            displayName = current.displayName.trim(),
                            userId = current.userId.trim()
                        )
                    )
                }
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

    // Google first-time users still route through Complete Profile.
    private suspend fun resolveDestination() {
        syncManager.startInitialSync()
        when (val result = postAuthNavigator.resolve()) {
            is AuthOutcome.Success -> _state.update { it.copy(isLoading = false, navTarget = result.data) }
            is AuthOutcome.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
        }
    }

    private fun setUserIdStatus(status: UserIdStatus) = _state.update { it.copy(userIdStatus = status) }

    companion object {
        private const val USER_ID_DEBOUNCE_MS = 500L
    }
}
