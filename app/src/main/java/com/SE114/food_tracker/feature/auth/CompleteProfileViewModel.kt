package com.SE114.food_tracker.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.data.repository.AuthError
import com.SE114.food_tracker.data.repository.AuthOutcome
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

enum class UserIdStatus { Idle, Invalid, Checking, Available, Taken, Error }

data class CompleteProfileUiState(
    val userId: String = "",
    val userIdStatus: UserIdStatus = UserIdStatus.Idle,
    val isSubmitting: Boolean = false,
    val error: AuthError? = null,
    val completed: Boolean = false
) {
    // Available => clear to submit. Error => the UX probe failed (e.g. offline); still
    // allow submit and let the DB's unique index be the authority. Everything else blocks.
    val canSubmit: Boolean
        get() = !isSubmitting &&
            (userIdStatus == UserIdStatus.Available || userIdStatus == UserIdStatus.Error)
}

@OptIn(FlowPreview::class)
@HiltViewModel
class CompleteProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CompleteProfileUiState())
    val state: StateFlow<CompleteProfileUiState> = _state.asStateFlow()

    private val userIdInput = MutableStateFlow("")

    init {
        viewModelScope.launch {
            userIdInput
                .debounce(USER_ID_DEBOUNCE_MS)
                .collectLatest { id ->
                    when {
                        id.isEmpty() -> setStatus(UserIdStatus.Idle)
                        !USER_ID_REGEX.matches(id) -> setStatus(UserIdStatus.Invalid)
                        else -> {
                            setStatus(UserIdStatus.Checking)
                            val status = when (val outcome = profileRepository.isUserIdAvailable(id)) {
                                is AuthOutcome.Success ->
                                    if (outcome.data) UserIdStatus.Available else UserIdStatus.Taken
                                is AuthOutcome.Failure -> UserIdStatus.Error
                            }
                            setStatus(status)
                        }
                    }
                }
        }
    }

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
            _state.update { it.copy(isSubmitting = true, error = null) }
            when (val outcome = profileRepository.completeOnboarding(current.userId.trim())) {
                is AuthOutcome.Success -> _state.update { it.copy(isSubmitting = false, completed = true) }
                is AuthOutcome.Failure -> _state.update { it.copy(isSubmitting = false, error = outcome.error) }
            }
        }
    }

    private fun setStatus(status: UserIdStatus) = _state.update { it.copy(userIdStatus = status) }

    companion object {
        private const val USER_ID_DEBOUNCE_MS = 500L
        val USER_ID_REGEX = Regex("^[a-z0-9._]{4,20}$")
    }
}
