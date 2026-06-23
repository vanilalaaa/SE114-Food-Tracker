package com.SE114.food_tracker.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.data.repository.ProfileViewerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileViewerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val profileId: String = savedStateHandle.get<String>("profileId").orEmpty()

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        if (profileId.isBlank()) {
            _uiState.update {
                it.copy(isLoading = false, error = "Không xác định được profile.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    diaryError = null
                )
            }

            val authId = profileRepository.currentAuthUserId()

            profileRepository.fetchProfile(profileId)
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = profile,
                            isSelf = profile.id == authId,
                            error = null
                        )
                    }
                    loadSharedDiary()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Không tải được profile."
                        )
                    }
                }
        }
    }

    fun loadSharedDiary() {
        if (profileId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDiaryLoading = true, diaryError = null) }

            profileRepository.fetchSharedItems(profileId)
                .onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            isDiaryLoading = false,
                            sharedItems = items,
                            diaryError = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isDiaryLoading = false,
                            diaryError = error.message ?: "Không tải được nhật ký."
                        )
                    }
                }
        }
    }

    fun selectTab(tab: ProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}
