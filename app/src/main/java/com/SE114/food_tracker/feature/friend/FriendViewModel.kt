package com.SE114.food_tracker.feature.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.data.remote.dto.ProfileDTO
import com.SE114.food_tracker.data.repository.AuthRepository
import com.SE114.food_tracker.data.repository.FeedRepository
import com.SE114.food_tracker.data.repository.FriendRepository
import com.SE114.food_tracker.data.repository.ReportRepository
import com.SE114.food_tracker.feature.report.ReportReason
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val repository: FriendRepository,
    private val feedRepository: FeedRepository,
    private val reportRepository: ReportRepository,
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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage = _actionMessage.asStateFlow()

    private val _isReportSubmitting = MutableStateFlow(false)
    val isReportSubmitting = _isReportSubmitting.asStateFlow()

    private val _busyFriendshipIds = MutableStateFlow<Set<String>>(emptySet())
    val busyFriendshipIds = _busyFriendshipIds.asStateFlow()

    private var loadFriendDataJob: Job? = null
    private var searchJob: Job? = null
    private var realtimeFriendshipRefreshJob: Job? = null
    private var autoFriendshipRefreshJob: Job? = null

    init {
        loadFriendData()
        subscribeToFriendshipRealtime()
        startAutoFriendshipRefresh()

        viewModelScope.launch {
            authRepository.currentSessionFlow()
                .filterIsInstance<SessionStatus.Authenticated>()
                .collect {
                    loadFriendData()
                }
        }
    }

    private fun subscribeToFriendshipRealtime() {
        repository.subscribeToFriendshipRealtime()
        viewModelScope.launch {
            repository.friendshipRealtimeEvents.collect {
                scheduleRealtimeFriendshipRefresh()
            }
        }
    }

    private fun scheduleRealtimeFriendshipRefresh() {
        realtimeFriendshipRefreshJob?.cancel()
        realtimeFriendshipRefreshJob = viewModelScope.launch {
            delay(500)
            refreshFriendDataFromRealtime()
        }
    }

    private suspend fun refreshFriendDataFromRealtime() {
        _profileLoadError.value = null
        repository.refreshCurrentProfile()
            .onSuccess {
                repository.refreshFriendships()
                    .onSuccess {
                        refreshSearchRelationshipSafely()
                        runCatching { feedRepository.refreshVisibleFromSupabase() }
                            .onFailure { reportActionError(it) }
                    }
                    .onFailure { reportActionError(it) }
            }
            .onFailure { e ->
                _profileLoadError.value = e.message ?: "Không lấy được ID"
            }
    }

    private fun startAutoFriendshipRefresh() {
        if (autoFriendshipRefreshJob != null) return

        autoFriendshipRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                if (_busyFriendshipIds.value.isEmpty() && !_isRefreshing.value) {
                    refreshFriendDataFromRealtime()
                }
            }
        }
    }

    fun refresh() = loadFriendData(showRefreshing = true)

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
            updateSearchRelationship(targetProfileId, FriendRelationship.PENDING)
            repository.sendFriendRequest(targetProfileId)
                .onSuccess {
                    refreshSearchRelationshipSafely()
                }
                .onFailure {
                    refreshSearchRelationshipSafely()
                    reportActionError(it)
                }
        }
    }

    fun acceptRequest(friendshipId: String) =
        runFriendAction(friendshipId) { repository.acceptRequest(friendshipId) }

    fun declineRequest(friendshipId: String) =
        runFriendAction(friendshipId) { repository.declineRequest(friendshipId) }

    fun unfriend(friendshipId: String) =
        runFriendAction(friendshipId) { repository.unfriend(friendshipId) }

    fun cancelOutgoingRequest(friendshipId: String) =
        runFriendAction(friendshipId) { repository.cancelOutgoingRequest(friendshipId) }

    fun submitReport(targetId: String, reason: ReportReason, details: String?) {
        if (_isReportSubmitting.value) return

        viewModelScope.launch {
            _isReportSubmitting.value = true
            reportRepository.submitProfileReport(
                targetId = targetId,
                reason = reason.toRemoteValue(details)
            ).onSuccess {
                _actionMessage.value = "Đã gửi báo cáo, admin sẽ xem xét"
            }.onFailure { reportActionError(it) }
            _isReportSubmitting.value = false
        }
    }

    private fun loadFriendData(showRefreshing: Boolean = false) {
        loadFriendDataJob?.cancel()
        loadFriendDataJob = viewModelScope.launch {
            try {
                if (showRefreshing) _isRefreshing.value = true
                _profileLoadError.value = null
                repository.refreshCurrentProfile()
                    .onSuccess {
                        repository.refreshFriendships()
                            .onSuccess { refreshSearchRelationshipSafely() }
                    }
                    .onFailure { e ->
                        _profileLoadError.value = e.message ?: "Không lấy được ID"
                    }
            } finally {
                if (showRefreshing) _isRefreshing.value = false
            }
        }
    }

    private fun runFriendAction(friendshipId: String, action: suspend () -> Result<Unit>) {
        if (friendshipId in _busyFriendshipIds.value) return
        _busyFriendshipIds.update { it + friendshipId }

        viewModelScope.launch {
            try {
                runCatching { action() }.getOrElse { Result.failure(it) }
                    .onSuccess {
                        repository.refreshFriendships()
                            .onSuccess { refreshSearchRelationshipSafely() }
                            .onFailure { reportActionError(it) }
                        runCatching { feedRepository.refreshVisibleFromSupabase() }
                            .onFailure { reportActionError(it) }
                    }
                    .onFailure { reportActionError(it) }
            } finally {
                _busyFriendshipIds.update { it - friendshipId }
            }
        }
    }

    private fun reportActionError(t: Throwable) {
        _actionMessage.value = t.message ?: "Đã xảy ra lỗi, vui lòng thử lại."
    }

    private suspend fun performSearch(query: String) {
        if (query != _searchQuery.value.trim()) return

        _isLoadingSearch.value = true
        try {
            _searchResult.value = repository.searchUser(query).mapCatching { profile ->
                FriendSearchResult(
                    profile = profile,
                    relationship = FriendRelationship.fromStatus(
                        repository.friendshipStatusWith(profile.id)
                    )
                )
            }
        } finally {
            _isLoadingSearch.value = false
        }
    }

    private suspend fun refreshSearchRelationship() {
        val currentSearch = _searchResult.value?.getOrNull() ?: return
        _searchResult.value = Result.success(
            currentSearch.copy(
                relationship = FriendRelationship.fromStatus(
                    repository.friendshipStatusWith(currentSearch.profile.id)
                )
            )
        )
    }

    private suspend fun refreshSearchRelationshipSafely() {
        runCatching { refreshSearchRelationship() }
            .onFailure { reportActionError(it) }
    }

    private fun updateSearchRelationship(profileId: String, relationship: FriendRelationship) {
        val currentSearch = _searchResult.value?.getOrNull() ?: return
        if (currentSearch.profile.id != profileId) return

        _searchResult.value = Result.success(
            currentSearch.copy(relationship = relationship)
        )
    }

    private companion object {
        const val MIN_SEARCH_ID_LENGTH = 3
        const val SEARCH_DEBOUNCE_MS = 450L
        const val AUTO_REFRESH_INTERVAL_MS = 5_000L
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
