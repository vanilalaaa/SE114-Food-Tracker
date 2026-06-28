package com.SE114.food_tracker.feature.profile

import android.content.Context
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.network.NetworkMonitor
import com.SE114.food_tracker.core.sync.SyncScheduler
import com.SE114.food_tracker.core.util.toUserFacingMessage
import com.SE114.food_tracker.data.local.dao.FeedPostDto
import com.SE114.food_tracker.data.repository.FeedPostDeleteSyncException
import com.SE114.food_tracker.data.repository.FeedRepository
import com.SE114.food_tracker.data.repository.FriendRepository
import com.SE114.food_tracker.data.repository.ProfileViewerRepository
import com.SE114.food_tracker.data.repository.ReportRepository
import com.SE114.food_tracker.feature.feed.feedImageModelOrNull
import com.SE114.food_tracker.feature.report.ReportReason
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URL

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileViewerRepository,
    private val feedRepository: FeedRepository,
    private val friendRepository: FriendRepository,
    private val reportRepository: ReportRepository,
    private val networkMonitor: NetworkMonitor,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val profileId: String = savedStateHandle.get<String>("profileId").orEmpty()
    private var postsJob: Job? = null
    private var selectedCommentsJob: Job? = null
    private var realtimePostsJob: Job? = null
    private var realtimeFriendshipJob: Job? = null
    private var realtimeFriendshipRefreshJob: Job? = null

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            currentUserId = feedRepository.currentUserId(),
            isLoading = true
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        subscribeToPostRealtime()
        subscribeToFriendshipRealtime()
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
            val cachedProfile = profileRepository.cachedProfile(profileId)
            if (cachedProfile != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profile = cachedProfile,
                        isSelf = cachedProfile.id == authId,
                        error = null
                    )
                }
                loadFriendship(cachedProfile.id, authId)
                loadSharedDiary()
                observeAuthorPosts()
            } else {
                observeAuthorPosts()
            }

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
                        if (it.profile != null) {
                            it.copy(isLoading = false, error = null)
                        } else {
                            it.copy(
                                isLoading = false,
                                error = error.toUserFacingMessage("Không tải được profile.")
                            )
                        }
                    }
                }
        }
    }

    fun loadSharedDiary() {
        if (profileId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDiaryLoading = true, diaryError = null) }
            val cachedItems = profileRepository.cachedSharedItems(profileId)
            if (cachedItems.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        isDiaryLoading = false,
                        sharedItems = cachedItems,
                        diaryError = null
                    )
                }
            }

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
                        if (it.sharedItems.isNotEmpty()) {
                            it.copy(isDiaryLoading = false, diaryError = null)
                        } else {
                            it.copy(
                                isDiaryLoading = false,
                                diaryError = error.toUserFacingMessage("Không tải được nhật ký.")
                            )
                        }
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
                    val selectedIndex = it.selectedPostId
                        ?.let { selectedPostId -> posts.indexOfFirst { post -> post.postId == selectedPostId } }
                        ?: -1
                    it.copy(
                        posts = posts,
                        isPostsLoading = false,
                        selectedPostIndex = selectedIndex,
                        selectedPostId = selectedIndex.takeIf { index -> index >= 0 }
                            ?.let { index -> posts[index].postId }
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

    fun openPostDetail(postId: String) {
        val posts = _uiState.value.posts
        val index = posts.indexOfFirst { it.postId == postId }
        if (index < 0) return

        _uiState.update {
            it.copy(
                selectedPostId = postId,
                selectedPostIndex = index
            )
        }
        observeSelectedComments(postId)
    }

    fun selectPostAt(index: Int) {
        val post = _uiState.value.posts.getOrNull(index) ?: return
        _uiState.update {
            it.copy(
                selectedPostId = post.postId,
                selectedPostIndex = index
            )
        }
        observeSelectedComments(post.postId)
    }

    fun closePostDetail() {
        selectedCommentsJob?.cancel()
        selectedCommentsJob = null
        _uiState.update {
            it.copy(
                selectedPostId = null,
                selectedPostIndex = -1,
                selectedComments = emptyList()
            )
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            if (!requireOnlineAction()) return@launch
            runCatching { feedRepository.toggleLike(postId) }
                .onSuccess { SyncScheduler.triggerImmediateSync(context) }
                .onFailure { error ->
                    Timber.e(error, "[ProfileVM] Toggle like failed")
                    showMessage(error.toUserFacingMessage("Không cập nhật được lượt thích"))
                }
        }
    }

    fun hidePost(postId: String) {
        viewModelScope.launch {
            if (!requireOnlineAction()) return@launch
            runCatching { feedRepository.hidePost(postId) }
                .onSuccess { closePostDetail() }
                .onFailure { error ->
                    Timber.e(error, "[ProfileVM] Hide post failed")
                    showMessage(error.toUserFacingMessage("Không ẩn được bài viết"))
                }
        }
    }

    fun downloadPostImage(post: FeedPostDto) {
        viewModelScope.launch {
            runCatching { savePostImageToGallery(post) }
                .onSuccess { showMessage("Đã tải ảnh về thư viện") }
                .onFailure { error ->
                    Timber.e(error, "[ProfileVM] Download post image failed")
                    showMessage(error.toUserFacingMessage("Không tải được ảnh"))
                }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            if (!requireOnlineAction()) return@launch
            runCatching { feedRepository.deletePost(postId) }
                .onSuccess { closePostDetail() }
                .onFailure { error ->
                    Timber.e(error, "[ProfileVM] Delete post failed")
                    if (error is FeedPostDeleteSyncException) {
                        closePostDetail()
                        SyncScheduler.triggerImmediateSync(context)
                    }
                    showMessage(error.toUserFacingMessage("Không xóa được bài viết"))
                }
        }
    }

    fun addComment(postId: String, body: String, parentCommentId: String? = null) {
        if (body.isBlank()) return
        viewModelScope.launch {
            if (!requireOnlineAction()) return@launch
            runCatching { feedRepository.addComment(postId, body, parentCommentId) }
                .onSuccess { SyncScheduler.triggerImmediateSync(context) }
                .onFailure { error ->
                    Timber.e(error, "[ProfileVM] Add comment failed")
                    showMessage(error.toUserFacingMessage("Không gửi được bình luận"))
                }
        }
    }

    fun editComment(commentId: String, body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            if (!requireOnlineAction()) return@launch
            runCatching { feedRepository.editComment(commentId, body) }
                .onSuccess { SyncScheduler.triggerImmediateSync(context) }
                .onFailure { error ->
                    Timber.e(error, "[ProfileVM] Edit comment failed")
                    showMessage(error.toUserFacingMessage("Không sửa được bình luận"))
                }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            if (!requireOnlineAction()) return@launch
            runCatching { feedRepository.deleteComment(commentId) }
                .onSuccess { SyncScheduler.triggerImmediateSync(context) }
                .onFailure { error ->
                    Timber.e(error, "[ProfileVM] Delete comment failed")
                    showMessage(error.toUserFacingMessage("Không xóa được bình luận"))
                }
        }
    }

    fun setCommentHidden(commentId: String, isHidden: Boolean) {
        viewModelScope.launch {
            if (!requireOnlineAction()) return@launch
            runCatching { feedRepository.setCommentHidden(commentId, isHidden) }
                .onSuccess { SyncScheduler.triggerImmediateSync(context) }
                .onFailure { error ->
                    Timber.e(error, "[ProfileVM] Toggle comment visibility failed")
                    showMessage(error.toUserFacingMessage("Không cập nhật được bình luận"))
                }
        }
    }

    fun submitReport(reason: ReportReason, details: String?) {
        if (profileId.isBlank() || _uiState.value.isSelf || _uiState.value.isReportSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isReportSubmitting = true, reportMessage = null) }

            reportRepository.submitProfileReport(
                targetId = profileId,
                reason = reason.toRemoteValue(details)
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
                        reportMessage = error.toUserFacingMessage("Không cập nhật được lời mời kết bạn.")
                    )
                }
            }
        }
    }

    fun clearReportMessage() {
        _uiState.update { it.copy(reportMessage = null) }
    }

    private fun observeSelectedComments(postId: String) {
        selectedCommentsJob?.cancel()
        selectedCommentsJob = viewModelScope.launch {
            val currentUserId = feedRepository.currentUserId()
            _uiState.update { it.copy(currentUserId = currentUserId) }
            feedRepository.observeComments(postId, currentUserId).collect { comments ->
                _uiState.update { it.copy(selectedComments = comments) }
            }
        }
    }

    private fun subscribeToFriendshipRealtime() {
        if (realtimeFriendshipJob != null) return

        friendRepository.subscribeToFriendshipRealtime()
        realtimeFriendshipJob = viewModelScope.launch {
            friendRepository.friendshipRealtimeEvents.collect {
                scheduleRealtimeFriendshipRefresh()
            }
        }
    }

    private fun scheduleRealtimeFriendshipRefresh() {
        realtimeFriendshipRefreshJob?.cancel()
        realtimeFriendshipRefreshJob = viewModelScope.launch {
            kotlinx.coroutines.delay(REALTIME_REFRESH_DEBOUNCE_MS)
            val targetProfileId = _uiState.value.profile?.id ?: profileId
            loadFriendship(targetProfileId, profileRepository.currentAuthUserId())
            runCatching { feedRepository.refreshVisibleFromSupabase() }
                .onFailure { Timber.e(it, "[ProfileVM] Realtime friendship refresh failed") }
        }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(reportMessage = message) }
    }

    private suspend fun requireOnlineAction(): Boolean {
        if (networkMonitor.isOnline.first()) return true
        showMessage("Cần có mạng để thao tác")
        return false
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
            rawMessage.isNotBlank() -> toUserFacingMessage("Không gửi được báo cáo. Vui lòng thử lại.")
            else -> "Không gửi được báo cáo. Vui lòng thử lại."
        }
    }

    private suspend fun savePostImageToGallery(post: FeedPostDto) {
        val imageModel = post.imageUrl.feedImageModelOrNull()
            ?: error("Bài viết này không có ảnh để tải.")

        withContext(Dispatchers.IO) {
            val bytes = when {
                imageModel.startsWith("http", ignoreCase = true) ->
                    URL(imageModel).openStream().use { it.readBytes() }

                else -> {
                    val uri = Uri.parse(imageModel)
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Không đọc được ảnh bài viết.")
                }
            }

            val filename = "food_tracker_${post.postId}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FoodTracker")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Không tạo được file ảnh trong thư viện.")

            runCatching {
                resolver.openOutputStream(uri)?.use { output -> output.write(bytes) }
                    ?: error("Không ghi được ảnh vào thư viện.")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
            }.onFailure { throwable ->
                resolver.delete(uri, null, null)
                throw throwable
            }.getOrThrow()
        }
    }

    private companion object {
        const val REALTIME_REFRESH_DEBOUNCE_MS = 120L
    }
}
