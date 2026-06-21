package com.SE114.food_tracker.feature.feed

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.sync.SyncScheduler
import com.SE114.food_tracker.data.local.dao.FeedCommentDto
import com.SE114.food_tracker.data.local.dao.FeedPostDto
import com.SE114.food_tracker.data.local.dao.FeedSourceItemDto
import com.SE114.food_tracker.data.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _page = MutableStateFlow(1)
    private val _selectedPostId = MutableStateFlow<String?>(null)
    private val _selectedPostIndex = MutableStateFlow(-1)
    private val _isCreateSheetOpen = MutableStateFlow(false)
    private val _selectedSourceItem = MutableStateFlow<FeedSourceItemDto?>(null)
    private val _pickedImageUri = MutableStateFlow<Uri?>(null)
    private val _draftCaption = MutableStateFlow("")
    private val _draftVisibility = MutableStateFlow(FeedVisibility.FRIENDS.value)
    private val _isLoading = MutableStateFlow(false)
    private val _isCreatingPost = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val posts = _page.flatMapLatest { page ->
        feedRepository.observePosts(pageSize = FeedRepository.PAGE_SIZE, page = page)
    }

    private val selectedComments = _selectedPostId.flatMapLatest { postId ->
        if (postId == null) flowOf(emptyList()) else feedRepository.observeComments(postId)
    }

    val uiState: StateFlow<FeedUiState> =
        combine(
            posts,
            feedRepository.observeSourceItems(),
            _selectedPostId,
            _selectedPostIndex,
            selectedComments,
            _page,
            _isCreateSheetOpen,
            _selectedSourceItem,
            _pickedImageUri,
            _draftCaption,
            _draftVisibility,
            _isLoading,
            _isCreatingPost,
            _error
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            FeedUiState(
                currentUserId = feedRepository.currentUserId(),
                posts = values[0] as List<FeedPostDto>,
                sourceItems = values[1] as List<FeedSourceItemDto>,
                selectedPostId = values[2] as String?,
                selectedPostIndex = values[3] as Int,
                selectedComments = values[4] as List<FeedCommentDto>,
                page = values[5] as Int,
                isCreateSheetOpen = values[6] as Boolean,
                selectedSourceItem = values[7] as FeedSourceItemDto?,
                pickedImageUri = values[8] as Uri?,
                draftCaption = values[9] as String,
                draftVisibility = values[10] as String,
                isLoading = values[11] as Boolean,
                isCreatingPost = values[12] as Boolean,
                error = values[13] as String?
            )
        }
            .catch { throwable ->
                Timber.e(throwable, "[FeedVM] Failed to build feed state")
                _error.value = throwable.message
                emit(FeedUiState(error = throwable.message))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = FeedUiState(isLoading = true)
            )

    fun loadNextPage() {
        if (_isLoading.value || !uiState.value.canLoadMore) return
        _page.value += 1
    }

    fun refresh() {
        _page.value = 1
        _error.value = null
    }

    fun openCreateSheet() {
        _isCreateSheetOpen.value = true
        _error.value = null
    }

    fun closeCreateSheet() {
        _isCreateSheetOpen.value = false
        clearDraft()
    }

    fun selectSourceItem(item: FeedSourceItemDto?) {
        _selectedSourceItem.value = item
        if (item != null) {
            _pickedImageUri.value = null
            if (_draftCaption.value.isBlank()) {
                _draftCaption.value = item.name
            }
        }
    }

    fun onImagePicked(uri: Uri?) {
        _pickedImageUri.value = uri
        if (uri != null) {
            _selectedSourceItem.value = null
        }
    }

    fun updateDraftCaption(caption: String) {
        _draftCaption.value = caption
    }

    fun updateDraftVisibility(visibility: FeedVisibility) {
        _draftVisibility.value = visibility.value
    }

    fun openPostDetail(postId: String) {
        val posts = uiState.value.posts
        _selectedPostId.value = postId
        _selectedPostIndex.value = posts.indexOfFirst { it.postId == postId }
    }

    fun selectPostAt(index: Int) {
        val post = uiState.value.posts.getOrNull(index) ?: return
        _selectedPostId.value = post.postId
        _selectedPostIndex.value = index
    }

    fun closePostDetail() {
        _selectedPostId.value = null
        _selectedPostIndex.value = -1
    }

    fun createPost() {
        viewModelScope.launch {
            _isCreatingPost.value = true
            _error.value = null

            try {
                val sourceItem = _selectedSourceItem.value
                val pickedImageUri = _pickedImageUri.value
                val caption = _draftCaption.value
                val visibility = _draftVisibility.value

                when {
                    sourceItem != null -> feedRepository.createPostFromItem(
                        item = sourceItem,
                        caption = caption,
                        visibility = visibility
                    )

                    pickedImageUri != null -> feedRepository.createPostFromImage(
                        imageUrl = copyPickedImageToFeedStorage(pickedImageUri),
                        caption = caption,
                        visibility = visibility
                    )

                    else -> _error.value = "Chọn món trong nhật ký hoặc chọn ảnh trước nha"
                }

                if (_error.value == null) {
                    closeCreateSheet()
                    refresh()
                    SyncScheduler.triggerImmediateSync(context)
                }
            } catch (throwable: Throwable) {
                Timber.e(throwable, "[FeedVM] Create post failed")
                _error.value = throwable.message ?: "Tạo bài viết thất bại"
            } finally {
                _isCreatingPost.value = false
            }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            runCatching { feedRepository.toggleLike(postId) }
                .onSuccess { SyncScheduler.triggerImmediateSync(context) }
                .onFailure { throwable ->
                    Timber.e(throwable, "[FeedVM] Toggle like failed")
                    _error.value = throwable.message ?: "Không cập nhật được lượt thích"
                }
        }
    }

    fun addComment(postId: String, body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            runCatching { feedRepository.addComment(postId, body) }
                .onSuccess { SyncScheduler.triggerImmediateSync(context) }
                .onFailure { throwable ->
                    Timber.e(throwable, "[FeedVM] Add comment failed")
                    _error.value = throwable.message ?: "Không gửi được bình luận"
                }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            runCatching { feedRepository.deletePost(postId) }
                .onSuccess {
                    closePostDetail()
                    SyncScheduler.triggerImmediateSync(context)
                }
                .onFailure { throwable ->
                    Timber.e(throwable, "[FeedVM] Delete post failed")
                    _error.value = throwable.message ?: "Không xóa được bài viết"
                }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun clearDraft() {
        _selectedSourceItem.value = null
        _pickedImageUri.value = null
        _draftCaption.value = ""
        _draftVisibility.value = FeedVisibility.FRIENDS.value
    }

    private suspend fun copyPickedImageToFeedStorage(uri: Uri): String =
        withContext(Dispatchers.IO) {
            val extension = context.contentResolver.getType(uri)
                ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                ?: "jpg"
            val directory = File(context.filesDir, "feed_posts").apply { mkdirs() }
            val target = File(directory, "${UUID.randomUUID()}.$extension")

            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Không đọc được ảnh đã chọn")

            Uri.fromFile(target).toString()
        }
}
