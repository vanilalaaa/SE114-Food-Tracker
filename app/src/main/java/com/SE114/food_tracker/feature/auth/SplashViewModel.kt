package com.SE114.food_tracker.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.sync.SyncManager
import com.SE114.food_tracker.data.repository.AuthError
import com.SE114.food_tracker.data.repository.AuthOutcome
import com.SE114.food_tracker.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class SplashUiState(
    val navTarget: PostAuthDestination? = null,
    val goToLogin: Boolean = false,
    val error: AuthError? = null
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val postAuthNavigator: PostAuthNavigator,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(SplashUiState())
    val state: StateFlow<SplashUiState> = _state.asStateFlow()

    init {
        decide()
    }

    fun retry() = decide()

    private fun decide() {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            // Show the logo while the stored session resolves, but never block longer than 1s.
            val status = withTimeoutOrNull(SPLASH_TIMEOUT_MS) {
                authRepository.currentSessionFlow().first { it !is SessionStatus.Initializing }
            }
            if (status is SessionStatus.Authenticated) {
                syncManager.startInitialSync()
                when (val result = postAuthNavigator.resolve()) {
                    is AuthOutcome.Success -> _state.update { it.copy(navTarget = result.data) }
                    is AuthOutcome.Failure -> _state.update { it.copy(error = result.error) }
                }
            } else {
                _state.update { it.copy(goToLogin = true) }
            }
        }
    }

    companion object {
        private const val SPLASH_TIMEOUT_MS = 1000L
    }
}
