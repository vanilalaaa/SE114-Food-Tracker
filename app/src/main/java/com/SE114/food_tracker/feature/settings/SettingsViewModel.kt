package com.SE114.food_tracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.sync.LocalDataCleaner
import com.SE114.food_tracker.data.repository.AuthOutcome
import com.SE114.food_tracker.data.repository.AuthRepository
import com.SE114.food_tracker.data.repository.ChatRepository
import com.SE114.food_tracker.data.repository.FeedRepository
import com.SE114.food_tracker.data.repository.FriendRepository
import com.SE114.food_tracker.data.repository.Profile
import com.SE114.food_tracker.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val feedRepository: FeedRepository,
    private val friendRepository: FriendRepository,
    private val localDataCleaner: LocalDataCleaner
) : ViewModel() {

    val profile: StateFlow<Profile?> = profileRepository.observeMyProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Gates the admin entry in Settings; the server re-checks is_admin on every admin RPC.
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    init {
        viewModelScope.launch { profileRepository.refreshMyProfile() }
        viewModelScope.launch {
            when (val outcome = profileRepository.amIAdmin()) {
                is AuthOutcome.Success -> _isAdmin.value = outcome.data
                is AuthOutcome.Failure -> Unit
            }
        }
    }

    /**
     * Explicit logout: wipe local user-owned data first, then sign out. The session
     * observer in MainScaffold reacts to NotAuthenticated and routes to login.
     */
    fun logout() {
        viewModelScope.launch {
            localDataCleaner.clearUserOwnedData()
            // Drop realtime channels while the session is still valid so a different account
            // logging in next rebuilds them cleanly (repositories are process singletons).
            chatRepository.resetChatState()
            feedRepository.resetFeedRealtime()
            friendRepository.resetFriendshipRealtime()
            authRepository.signOut()
        }
    }
}
