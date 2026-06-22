package com.SE114.food_tracker.feature.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.data.remote.dto.ProfileDTO
import com.SE114.food_tracker.data.repository.AuthRepository
import com.SE114.food_tracker.data.repository.FeedRepository
import com.SE114.food_tracker.data.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val repository: FriendRepository,
    private val feedRepository: FeedRepository,
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

    private val _searchResult = MutableStateFlow<Result<FriendSearchResult>?>(null)
    val searchResult = _searchResult.asStateFlow()

    private val _isLoadingSearch = MutableStateFlow(false)
    val isLoadingSearch = _isLoadingSearch.asStateFlow()

    private val _profileLoadError = MutableStateFlow<String?>(null)
    val profileLoadError = _profileLoadError.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage = _actionMessage.asStateFlow()

    private var loadFriendDataJob: Job? = null
    private var searchJob: Job? = null

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

    fun retryLoadProfile() = loadFriendData()

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.trim().length < MIN_SEARCH_ID_LENGTH) {
            _searchResult.value = null
            _isLoadingSearch.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            performSearch(query.trim())
        }
    }

    fun searchUser() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            searchJob?.cancel()
            performSearch(query)
        }
    }

    fun sendFriendRequest(targetProfileId: String) {
        viewModelScope.launch {
            repository.sendFriendRequest(targetProfileId)
                .onSuccess {
                    searchUser()
                }
                .onFailure { reportActionError(it) }
        }
    }

    fun acceptRequest(friendshipId: String) = runFriendAction { repository.acceptRequest(friendshipId) }

    fun declineRequest(friendshipId: String) = runFriendAction { repository.declineRequest(friendshipId) }

    fun unfriend(friendshipId: String) = runFriendAction { repository.unfriend(friendshipId) }

    fun cancelOutgoingRequest(friendshipId: String) = runFriendAction { repository.cancelOutgoingRequest(friendshipId) }

    private fun loadFriendData() {
        loadFriendDataJob?.cancel()
        loadFriendDataJob = viewModelScope.launch {
            _profileLoadError.value = null
            repository.refreshCurrentProfile()
                .onSuccess {
                    repository.refreshFriendships()
                }
                .onFailure { e ->
                    _profileLoadError.value = e.message ?: "Không lấy được ID"
                }
        }
    }

    private fun runFriendAction(action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            action()
                .onSuccess { runCatching { feedRepository.refreshVisibleFromSupabase() } }
                .onFailure { reportActionError(it) }
        }
    }

    private fun reportActionError(t: Throwable) {
        _actionMessage.value = t.message ?: "Đã xảy ra lỗi, vui lòng thử lại."
    }

    private suspend fun performSearch(query: String) {
        if (query != _searchQuery.value.trim()) return

        _isLoadingSearch.value = true
        _searchResult.value = repository.searchUser(query).map { profile ->
            FriendSearchResult(
                profile = profile,
                relationship = FriendRelationship.fromStatus(
                    repository.friendshipStatusWith(profile.id)
                )
            )
        }
        _isLoadingSearch.value = false
    }

    private companion object {
        const val MIN_SEARCH_ID_LENGTH = 3
        const val SEARCH_DEBOUNCE_MS = 450L
    }
}

data class FriendSearchResult(
    val profile: ProfileDTO,
    val relationship: FriendRelationship
)

enum class FriendRelationship(
    val label: String?,
    val canSendRequest: Boolean
) {
    NONE(label = null, canSendRequest = true),
    FRIENDS(label = "Bạn bè", canSendRequest = false),
    PENDING(label = "Đang chờ", canSendRequest = false);

    companion object {
        fun fromStatus(status: String?): FriendRelationship =
            when (status) {
                "accepted" -> FRIENDS
                "pending" -> PENDING
                else -> NONE
            }
    }
}
