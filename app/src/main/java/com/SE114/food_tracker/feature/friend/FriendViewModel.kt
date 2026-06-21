package com.SE114.food_tracker.feature.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.data.repository.FriendRepository
import com.SE114.food_tracker.data.remote.dto.ProfileDTO
import com.SE114.food_tracker.feature.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val repository: FriendRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val acceptedFriends = repository.getAcceptedFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomingRequests = repository.getIncomingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outgoingRequests = repository.getOutgoingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentProfile = repository.currentProfile

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResult = MutableStateFlow<Result<ProfileDTO>?>(null)
    val searchResult = _searchResult.asStateFlow()

    private val _isLoadingSearch = MutableStateFlow(false)
    val isLoadingSearch = _isLoadingSearch.asStateFlow()

    private val _profileLoadError = MutableStateFlow<String?>(null)
    val profileLoadError = _profileLoadError.asStateFlow()

    init {
        loadFriendData()

        viewModelScope.launch {
            authRepository.currentSessionFlow()
                .filterIsInstance<SessionStatus.Authenticated>()
                .collect {
                    loadFriendData()
                }
        }
    }

    private fun loadFriendData() {
        viewModelScope.launch {
            repository.refreshCurrentProfile()
                .onSuccess {
                    _profileLoadError.value = null
                    repository.refreshFriendships()
                }
                .onFailure {
                    _profileLoadError.value = "Không lấy được ID"
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResult.value = null
        }
    }

    fun searchUser() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _isLoadingSearch.value = true
            _searchResult.value = repository.searchUser(query)
            _isLoadingSearch.value = false
        }
    }

    fun sendFriendRequest(targetProfileId: String) {
        viewModelScope.launch {
            repository.sendFriendRequest(targetProfileId)
                .onSuccess {
                    _searchResult.value = null
                    _searchQuery.value = ""
                }
        }
    }

    fun acceptRequest(friendshipId: String) {
        viewModelScope.launch {
            repository.acceptRequest(friendshipId)
        }
    }

    fun declineRequest(friendshipId: String) {
        viewModelScope.launch {
            repository.declineRequest(friendshipId)
        }
    }

    fun unfriend(friendshipId: String) {
        viewModelScope.launch {
            repository.unfriend(friendshipId)
        }
    }

    fun cancelOutgoingRequest(friendshipId: String) {
        viewModelScope.launch {
            repository.cancelOutgoingRequest(friendshipId)
        }
    }
}
