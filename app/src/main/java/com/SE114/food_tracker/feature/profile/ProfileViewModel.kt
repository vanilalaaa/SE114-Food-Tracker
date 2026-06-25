package com.SE114.food_tracker.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.data.repository.FeedRepository
import com.SE114.food_tracker.data.repository.FriendRepository
import com.SE114.food_tracker.data.repository.ProfileViewerRepository
import com.SE114.food_tracker.data.repository.ReportRepository
import com.SE114.food_tracker.feature.report.ReportReason
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileViewerRepository,
    private val feedRepository: FeedRepository,
    private val friendRepository: FriendRepository,
    private val reportRepository: ReportRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val profileId: String = savedStateHandle.get<String>("profileId").orEmpty()
    private var postsJob: Job? = null
    private var realtimePostsJob: Job? = null

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        subscribeToPostRealtime()
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
                    loadFriendship(profile.id, authId)
                    loadSharedDiary()
                    observeAuthorPosts()
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

    private fun observeAuthorPosts() {
        if (profileId.isBlank()) return

        postsJob?.cancel()
        postsJob = viewModelScope.launch {
            _uiState.update { it.copy(isPostsLoading = true) }

            feedRepository.observePostsByAuthor(
                ownerId = profileId,
                pageSize = FeedRepository.PAGE_SIZE,
                page = 1
            ).collect { posts ->
                _uiState.update {
                    it.copy(
                        posts = posts,
                        isPostsLoading = false
                    )
                }
            }
        }
    }

    private fun subscribeToPostRealtime() {
        if (realtimePostsJob != null) return

        feedRepository.subscribeToFeedRealtime()
        realtimePostsJob = viewModelScope.launch {
            feedRepository.postRealtimeEvents.collect {
                runCatching { feedRepository.refreshVisibleFromSupabase() }
                    .onFailure { Timber.e(it, "[ProfileVM] Realtime profile posts refresh failed") }
            }
        }
    }

    fun selectTab(tab: ProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun submitReport(reason: ReportReason) {
        if (profileId.isBlank() || _uiState.value.isSelf || _uiState.value.isReportSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isReportSubmitting = true, reportMessage = null) }

            reportRepository.submitProfileReport(
                targetId = profileId,
                reason = reason.remoteValue
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isReportSubmitting = false,
                        reportMessage = "Đã gửi báo cáo, admin sẽ xem xét"
                    )
                }
            }.onFailure { error ->
                loadFriendship(profileId, profileRepository.currentAuthUserId())
                _uiState.update {
                    it.copy(
                        isReportSubmitting = false,
                        reportMessage = error.toReportMessage()
                    )
                }
            }
        }
    }

    fun performFriendshipAction() {
        val state = _uiState.value
        val friendshipId = state.friendshipId ?: return
        if (state.isFriendshipActionSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isFriendshipActionSubmitting = true, reportMessage = null) }
            _uiState.update {
                it.copy(
                    friendshipId = null,
                    friendshipStatus = null,
                    isFriendshipOutgoing = false
                )
            }

            val result = when (state.friendshipStatus) {
                "accepted" -> friendRepository.unfriend(friendshipId)
                "pending" -> {
                    if (state.isFriendshipOutgoing) {
                        friendRepository.cancelOutgoingRequest(friendshipId)
                    } else {
                        friendRepository.declineRequest(friendshipId)
                    }
                }
                else -> Result.success(Unit)
            }

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isFriendshipActionSubmitting = false,
                        reportMessage = "Đã cập nhật lời mời kết bạn"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isFriendshipActionSubmitting = false,
                        reportMessage = error.message ?: "Không cập nhật được lời mời kết bạn."
                    )
                }
            }
        }
    }

    fun clearReportMessage() {
        _uiState.update { it.copy(reportMessage = null) }
    }

    private suspend fun loadFriendship(targetProfileId: String, currentProfileId: String?) {
        if (currentProfileId == null || targetProfileId == currentProfileId) return

        runCatching { friendRepository.friendshipWith(targetProfileId) }
            .onSuccess { friendship ->
                _uiState.update {
                    it.copy(
                        friendshipId = friendship?.friendshipId,
                        friendshipStatus = friendship?.status,
                        isFriendshipOutgoing = friendship?.senderId == currentProfileId
                    )
                }
            }
    }

    private fun Throwable.toReportMessage(): String {
        val rawMessage = message.orEmpty()
        return when {
            rawMessage.contains("duplicate", ignoreCase = true) ||
                rawMessage.contains("unique", ignoreCase = true) ->
                "Bạn đã gửi báo cáo cho người dùng này rồi."
            rawMessage.contains("permission denied", ignoreCase = true) ||
                rawMessage.contains("row-level security", ignoreCase = true) ->
                "Chưa có quyền gửi báo cáo. Kiểm tra RLS Supabase."
            rawMessage.isNotBlank() -> rawMessage
            else -> "Không gửi được báo cáo. Vui lòng thử lại."
        }
    }
}
