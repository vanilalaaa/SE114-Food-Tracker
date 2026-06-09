package com.SE114.food_tracker.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.data.repository.AuthError
import com.SE114.food_tracker.data.repository.AuthOutcome
import com.SE114.food_tracker.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompleteProfileUiState(
    val userId: String = "",
    val isSubmitting: Boolean = false,
    val error: AuthError? = null,
    val completed: Boolean = false
) {
    val isFormatValid: Boolean get() = CompleteProfileViewModel.USER_ID_REGEX.matches(userId.trim())
    val showFormatError: Boolean get() = userId.isNotEmpty() && !isFormatValid
    val canSubmit: Boolean get() = isFormatValid && !isSubmitting
}

@HiltViewModel
class CompleteProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CompleteProfileUiState())
    val state: StateFlow<CompleteProfileUiState> = _state.asStateFlow()

    fun onUserIdChange(value: String) = _state.update { it.copy(userId = value, error = null) }

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

    companion object {
        val USER_ID_REGEX = Regex("^[a-z0-9._]{4,20}$")
    }
}
