package com.SE114.food_tracker.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.sync.SyncManager
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

enum class SplashDestination { Diary, Login }

data class SplashUiState(val destination: SplashDestination? = null)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(SplashUiState())
    val state: StateFlow<SplashUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Show the logo while the stored session resolves, but never block longer than 1s.
            val status = withTimeoutOrNull(SPLASH_TIMEOUT_MS) {
                authRepository.currentSessionFlow().first { it !is SessionStatus.Initializing }
            }
            val authenticated = status is SessionStatus.Authenticated
            if (authenticated) syncManager.startInitialSync()
            _state.update {
                it.copy(destination = if (authenticated) SplashDestination.Diary else SplashDestination.Login)
            }
        }
    }

    companion object {
        private const val SPLASH_TIMEOUT_MS = 1000L
    }
}
