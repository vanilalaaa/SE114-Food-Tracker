package com.SE114.food_tracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.sync.LocalDataCleaner
import com.SE114.food_tracker.data.repository.AuthRepository
import com.SE114.food_tracker.data.repository.Profile
import com.SE114.food_tracker.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val localDataCleaner: LocalDataCleaner
) : ViewModel() {

    val profile: StateFlow<Profile?> = profileRepository.observeMyProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { profileRepository.refreshMyProfile() }
    }

    /**
     * Explicit logout: wipe local user-owned data first, then sign out. The session
     * observer in MainScaffold reacts to NotAuthenticated and routes to login.
     */
    fun logout() {
        viewModelScope.launch {
            localDataCleaner.clearUserOwnedData()
            authRepository.signOut()
        }
    }
}
