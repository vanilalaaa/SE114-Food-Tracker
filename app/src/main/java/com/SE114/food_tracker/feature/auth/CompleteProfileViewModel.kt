package com.SE114.food_tracker.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.data.repository.AuthError
import com.SE114.food_tracker.data.repository.AuthOutcome
import com.SE114.food_tracker.data.repository.ImageRepository
import com.SE114.food_tracker.data.repository.Profile
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
    val displayName: String = "",
    val avatarUrl: String? = null,
    val userId: String = "",
    val userIdStatus: UserIdStatus = UserIdStatus.Idle,
    val isUploadingAvatar: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: AuthError? = null,
    val completed: Boolean = false
) {
    val displayNameValid: Boolean get() = displayName.isNotBlank()

    // Available => clear to submit. Error => the UX probe failed (e.g. offline); still
    // allow submit and let the DB's unique index be the authority. Everything else blocks.
    val canSubmit: Boolean
        get() = !isSubmitting && !isUploadingAvatar && displayNameValid &&
            (userIdStatus == UserIdStatus.Available || userIdStatus == UserIdStatus.Error)
}

@OptIn(FlowPreview::class)
@HiltViewModel
class CompleteProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CompleteProfileUiState())
    val state: StateFlow<CompleteProfileUiState> = _state.asStateFlow()

    private val userIdInput = MutableStateFlow("")
    private var profileId: String? = null
    private var pendingAvatarUrl: String? = null
    private var prefilled = false

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

        // Pre-fill name + avatar from the trigger-populated profile row (Google: name +
        // picture; email: the name typed at register). user_id is intentionally left blank.
        viewModelScope.launch {
            profileRepository.observeMyProfile().collect { profile ->
                profile?.let(::applyPrefill)
            }
        }
        viewModelScope.launch { profileRepository.refreshMyProfile() }
    }

    fun onDisplayNameChange(value: String) = _state.update { it.copy(displayName = value, error = null) }

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

    fun onAvatarPicked(bytes: ByteArray) {
        val uid = profileId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isUploadingAvatar = true, error = null) }
            imageRepository.uploadAvatar(uid, bytes).fold(
                onSuccess = { url ->
                    pendingAvatarUrl = url
                    _state.update { it.copy(isUploadingAvatar = false, avatarUrl = url) }
                },
                onFailure = { throwable ->
                    _state.update { it.copy(isUploadingAvatar = false, error = AuthError.Unknown(throwable.message)) }
                }
            )
        }
    }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            val outcome = profileRepository.completeOnboarding(
                displayName = current.displayName.trim(),
                userId = current.userId.trim(),
                avatarUrl = pendingAvatarUrl
            )
            when (outcome) {
                is AuthOutcome.Success -> _state.update { it.copy(isSubmitting = false, completed = true) }
                is AuthOutcome.Failure -> _state.update { it.copy(isSubmitting = false, error = outcome.error) }
            }
        }
    }

    private fun applyPrefill(p: Profile) {
        profileId = p.id
        if (prefilled) {
            _state.update { it.copy(avatarUrl = pendingAvatarUrl ?: p.avatarUrl) }
            return
        }
        prefilled = true
        _state.update {
            it.copy(
                displayName = it.displayName.ifBlank { p.displayName.orEmpty() },
                avatarUrl = pendingAvatarUrl ?: p.avatarUrl
            )
        }
    }

    private fun setStatus(status: UserIdStatus) = _state.update { it.copy(userIdStatus = status) }

    companion object {
        private const val USER_ID_DEBOUNCE_MS = 500L
        val USER_ID_REGEX = Regex("^[a-z0-9._]{4,20}$")
    }
}
