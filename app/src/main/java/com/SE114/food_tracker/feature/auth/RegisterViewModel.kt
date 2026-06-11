package com.SE114.food_tracker.feature.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.sync.SyncManager
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

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val userId: String = "",
    val userIdStatus: UserIdStatus = UserIdStatus.Idle,
    val isLoading: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val registered: Boolean = false
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() &&
            password.isNotBlank() &&
            displayName.isNotBlank() &&
            userIdStatus == UserIdStatus.Available &&
            !isLoading
}

@OptIn(FlowPreview::class)
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
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
                        !USER_ID_REGEX.matches(id) -> setUserIdStatus(UserIdStatus.Invalid)
                        else -> {
                            setUserIdStatus(UserIdStatus.Checking)
                            val status = runCatching { authRepository.isUserIdAvailable(id) }
                                .fold(
                                    onSuccess = { if (it) UserIdStatus.Available else UserIdStatus.Taken },
                                    onFailure = { UserIdStatus.Error }
                                )
                            setUserIdStatus(status)
                        }
                    }
                }
        }
    }

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, errorRes = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, errorRes = null) }
    fun onDisplayNameChange(value: String) = _state.update { it.copy(displayName = value, errorRes = null) }

    fun onUserIdChange(value: String) {
        _state.update {
            it.copy(
                userId = value,
                userIdStatus = if (value.isEmpty()) UserIdStatus.Idle else UserIdStatus.Checking,
                errorRes = null
            )
        }
        userIdInput.value = value
    }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorRes = null) }
            authRepository.signUp(
                email = current.email.trim(),
                password = current.password,
                displayName = current.displayName.trim(),
                userId = current.userId.trim()
            ).onSuccess {
                syncManager.startInitialSync()
                _state.update { it.copy(isLoading = false, registered = true) }
            }.onFailure {
                _state.update { it.copy(isLoading = false, errorRes = R.string.auth_register_error) }
            }
        }
    }

    private fun setUserIdStatus(status: UserIdStatus) =
        _state.update { it.copy(userIdStatus = status) }

    companion object {
        private const val USER_ID_DEBOUNCE_MS = 500L
        private val USER_ID_REGEX = Regex("^[a-z0-9._]{4,20}$")
    }
}
