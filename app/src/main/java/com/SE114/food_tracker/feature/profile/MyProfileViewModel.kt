package com.SE114.food_tracker.feature.profile

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

enum class UserIdCheckStatus { Idle, Invalid, Checking, Available, Taken, Error }

data class MyProfileUiState(
    val loading: Boolean = true,
    val displayName: String = "",
    val userId: String = "",
    val originalUserId: String = "",
    val originalDisplayName: String = "",
    val avatarUrl: String? = null,
    val userIdEditable: Boolean = false,
    val cooldownDays: Int = 0,
    val userIdStatus: UserIdCheckStatus = UserIdCheckStatus.Idle,
    val isUploadingAvatar: Boolean = false,
    val isSaving: Boolean = false,
    val saveSucceeded: Boolean = false,
    val error: AuthError? = null
) {
    val displayNameValid: Boolean get() = displayName.isNotBlank()
    val userIdChanged: Boolean get() = userIdEditable && userId.trim() != originalUserId
    private val userIdOkForSave: Boolean
        get() = !userIdChanged ||
            userIdStatus == UserIdCheckStatus.Available ||
            userIdStatus == UserIdCheckStatus.Error
    val canSave: Boolean
        get() = !loading && !isSaving && !isUploadingAvatar && displayNameValid && userIdOkForSave
}

@OptIn(FlowPreview::class)
@HiltViewModel
class MyProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MyProfileUiState())
    val state: StateFlow<MyProfileUiState> = _state.asStateFlow()

    private val userIdInput = MutableStateFlow("")
    private var profileId: String? = null
    private var pendingAvatarUrl: String? = null
    private var loadedAvatarUrl: String? = null

    init {
        viewModelScope.launch {
            userIdInput.debounce(USER_ID_DEBOUNCE_MS).collectLatest { value ->
                val id = value.trim()
                when {
                    id == _state.value.originalUserId || id.isEmpty() -> setStatus(UserIdCheckStatus.Idle)
                    !USER_ID_REGEX.matches(id) -> setStatus(UserIdCheckStatus.Invalid)
                    else -> {
                        setStatus(UserIdCheckStatus.Checking)
                        val status = when (val outcome = profileRepository.isUserIdAvailable(id)) {
                            is AuthOutcome.Success ->
                                if (outcome.data) UserIdCheckStatus.Available else UserIdCheckStatus.Taken
                            is AuthOutcome.Failure -> UserIdCheckStatus.Error
                        }
                        setStatus(status)
                    }
                }
            }
        }

        viewModelScope.launch {
            profileRepository.observeMyProfile().collect { profile ->
                profile?.let(::applyProfile)
            }
        }
        viewModelScope.launch { profileRepository.refreshMyProfile() }
        viewModelScope.launch { refreshCooldown() }
    }

    fun onDisplayNameChange(value: String) = _state.update { it.copy(displayName = value, error = null) }

    /** Revert unsaved edits (name/user_id/avatar) when leaving edit mode without saving. */
    fun discardEdits() {
        pendingAvatarUrl = null
        _state.update {
            it.copy(
                displayName = it.originalDisplayName,
                userId = it.originalUserId,
                avatarUrl = loadedAvatarUrl,
                userIdStatus = UserIdCheckStatus.Idle,
                error = null
            )
        }
        userIdInput.value = _state.value.originalUserId
    }

    fun consumeSaveSuccess() = _state.update { it.copy(saveSucceeded = false) }

    fun onUserIdChange(value: String) {
        if (!_state.value.userIdEditable) return
        _state.update {
            val pending = value.trim() != it.originalUserId && value.isNotEmpty()
            it.copy(
                userId = value,
                userIdStatus = if (pending) UserIdCheckStatus.Checking else UserIdCheckStatus.Idle,
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

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val newUserId = if (current.userIdChanged) current.userId.trim() else null
            when (val outcome = profileRepository.updateProfile(current.displayName.trim(), newUserId, pendingAvatarUrl)) {
                is AuthOutcome.Success -> {
                    pendingAvatarUrl = null
                    _state.update {
                        it.copy(
                            isSaving = false,
                            originalUserId = it.userId.trim(),
                            originalDisplayName = it.displayName.trim(),
                            userIdStatus = UserIdCheckStatus.Idle,
                            saveSucceeded = true
                        )
                    }
                    refreshCooldown()
                }
                is AuthOutcome.Failure -> _state.update { it.copy(isSaving = false, error = outcome.error) }
            }
        }
    }

    private fun applyProfile(p: Profile) {
        profileId = p.id
        loadedAvatarUrl = p.avatarUrl
        _state.update { s ->
            if (s.loading) {
                s.copy(
                    loading = false,
                    displayName = p.displayName.orEmpty(),
                    userId = p.userId.orEmpty(),
                    originalUserId = p.userId.orEmpty(),
                    originalDisplayName = p.displayName.orEmpty(),
                    avatarUrl = p.avatarUrl
                )
            } else {
                s.copy(
                    originalUserId = p.userId.orEmpty(),
                    originalDisplayName = p.displayName.orEmpty(),
                    avatarUrl = pendingAvatarUrl ?: p.avatarUrl
                )
            }
        }
    }

    private suspend fun refreshCooldown() {
        val days = (profileRepository.userIdCooldownRemaining() as? AuthOutcome.Success)?.data ?: 0
        _state.update { it.copy(userIdEditable = days <= 0, cooldownDays = days) }
    }

    private fun setStatus(status: UserIdCheckStatus) = _state.update { it.copy(userIdStatus = status) }

    companion object {
        private const val USER_ID_DEBOUNCE_MS = 500L
        val USER_ID_REGEX = Regex("^[a-z0-9._]{4,20}$")
    }
}
