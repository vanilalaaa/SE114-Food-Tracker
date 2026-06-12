package com.SE114.food_tracker.feature.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.data.repository.FriendRepository
import com.SE114.food_tracker.data.remote.dto.ProfileDTO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val repository: FriendRepository
) : ViewModel() {

    val acceptedFriends = repository.getAcceptedFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomingRequests = repository.getIncomingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outgoingRequests = repository.getOutgoingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResult = MutableStateFlow<Result<ProfileDTO>?>(null)
    val searchResult = _searchResult.asStateFlow()

    private val _isLoadingSearch = MutableStateFlow(false)
    val isLoadingSearch = _isLoadingSearch.asStateFlow()

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
            _searchResult.value = null
            _searchQuery.value = ""
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
}