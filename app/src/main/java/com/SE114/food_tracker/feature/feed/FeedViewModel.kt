package com.SE114.food_tracker.feature.feed

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.exifinterface.media.ExifInterface
import com.SE114.food_tracker.core.sync.SyncScheduler
import com.SE114.food_tracker.data.local.dao.FeedCommentDto
import com.SE114.food_tracker.data.local.dao.FeedPostDto
import com.SE114.food_tracker.data.local.dao.FeedSourceItemDto
import com.SE114.food_tracker.data.repository.FeedPostDeleteSyncException
import com.SE114.food_tracker.data.repository.FeedRepository
import com.SE114.food_tracker.data.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

private const val FeedPostImageMaxSide = 1280
private const val FeedPostImageQuality = 80
private const val MinFeedPostImageQuality = 60
private const val MaxFeedPostImageBytes = 1 * 1024 * 1024
private const val MaxFeedPostDecodeSize = 2048

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val friendRepository: FriendRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _page = MutableStateFlow(1)
    private val _selectedPostId = MutableStateFlow<String?>(null)
    private val _selectedPostIndex = MutableStateFlow(-1)
    private val _isCreateSheetOpen = MutableStateFlow(false)
    private val _selectedSourceItem = MutableStateFlow<FeedSourceItemDto?>(null)
    private val _pickedImageUri = MutableStateFlow<Uri?>(null)
    private val _draftFreeImageTitle = MutableStateFlow("")
    private val _draftCaption = MutableStateFlow("")
    private val _draftVisibility = MutableStateFlow(FeedVisibility.FRIENDS.value)
    private val _isLoading = MutableStateFlow(false)
    private val _isCreatingPost = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _currentUserId = MutableStateFlow(feedRepository.currentUserId())
    private var realtimeRefreshJob: Job? = null
    private var autoRefreshJob: Job? = null

    private val posts = combine(_page, _currentUserId) { page, currentUserId -> page to currentUserId }
        .flatMapLatest { (page, currentUserId) ->
            feedRepository.observePosts(
                pageSize = FeedRepository.PAGE_SIZE,
                page = page,
                currentUserId = currentUserId
            )
        }

    private val selectedComments = combine(_selectedPostId, _currentUserId) { postId, currentUserId ->
        postId to currentUserId
    }.flatMapLatest { (postId, currentUserId) ->
        if (postId == null) flowOf(emptyList()) else feedRepository.observeComments(postId, currentUserId)
    }

    val uiState: StateFlow<FeedUiState> =
        combine(
            posts,
            _currentUserId,
            feedRepository.observeSourceItems(),
            _selectedPostId,
            _selectedPostIndex,
            selectedComments,
            _page,
            _isCreateSheetOpen,
            _selectedSourceItem,
            _pickedImageUri,
            _draftFreeImageTitle,
            _draftCaption,
            _draftVisibility,
            _isLoading,
            _isCreatingPost,
            _error
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            FeedUiState(
                currentUserId = values[1] as String,
                posts = values[0] as List<FeedPostDto>,
                sourceItems = values[2] as List<FeedSourceItemDto>,
                selectedPostId = values[3] as String?,
                selectedPostIndex = values[4] as Int,
                selectedComments = values[5] as List<FeedCommentDto>,
                page = values[6] as Int,
                isCreateSheetOpen = values[7] as Boolean,
                selectedSourceItem = values[8] as FeedSourceItemDto?,
                pickedImageUri = values[9] as Uri?,
                draftFreeImageTitle = values[10] as String,
                draftCaption = values[11] as String,
                draftVisibility = values[12] as String,
                isLoading = values[13] as Boolean,
                isCreatingPost = values[14] as Boolean,
                error = values[15] as String?
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

    init {
        subscribeToRealtimePosts()
        subscribeToRealtimeFriendships()
        startAutoRefresh()
    }

    fun loadNextPage() {
        if (_isLoading.value || !uiState.value.canLoadMore) return
        _page.value += 1
    }

    fun refresh() {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            syncCurrentUserId()
            _page.value = 1
            _error.value = null

            runCatching {
                friendRepository.refreshCurrentProfile().getOrThrow()
                friendRepository.refreshFriendships().getOrThrow()
                syncCurrentUserId()
                if (!feedRepository.refreshVisibleFromSupabase()) {
                    error("Không làm mới được bảng tin")
                }
            }.onFailure { throwable ->
                Timber.e(throwable, "[FeedVM] Refresh failed")
                _error.value = throwable.message ?: "Không làm mới được bảng tin"
            }

            _isLoading.value = false
        }
    }

    private fun resetPage() {
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
            _draftFreeImageTitle.value = ""
            if (_draftCaption.value.isBlank()) {
                _draftCaption.value = item.name
            }
        }
    }

    fun onImagePicked(uri: Uri?) {
        _pickedImageUri.value = uri
        if (uri != null) {
            _selectedSourceItem.value = null
            _draftCaption.value = ""
            if (_draftFreeImageTitle.value.isBlank()) {
                _draftFreeImageTitle.value = "Ảnh của tôi"
            }
        }
    }

    fun updateDraftFreeImageTitle(title: String) {
        _draftFreeImageTitle.value = title.take(MaxPostTitleLength)
    }

    fun updateDraftCaption(caption: String) {
        _draftCaption.value = caption.take(MaxPostCaptionLength)
    }

    fun updateDraftVisibility(visibility: FeedVisibility) {
        _draftVisibility.value = when (visibility) {
            FeedVisibility.PUBLIC -> FeedVisibility.FRIENDS.value
            else -> visibility.value
        }
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
                val freeImageTitle = _draftFreeImageTitle.value
                val caption = _draftCaption.value
                val visibility = _draftVisibility.value
                    .takeUnless { it == FeedVisibility.PUBLIC.value }
                    ?: FeedVisibility.FRIENDS.value
                var createdOwnerId: String? = null

                when {
                    sourceItem != null -> {
                        createdOwnerId = feedRepository.createPostFromItem(
                            item = sourceItem,
                            caption = caption,
                            visibility = visibility
                        )
                    }

                    pickedImageUri != null -> {
                        if (freeImageTitle.isBlank()) {
                            _error.value = "Nhập tên loại ảnh trước khi đăng"
                        } else {
                            createdOwnerId = feedRepository.createPostFromImage(
                                imageUrl = copyPickedImageToFeedStorage(pickedImageUri),
                                caption = buildFreeImageCaption(freeImageTitle, caption),
                                visibility = visibility
                            )
                        }
                    }

                    else -> _error.value = "Chọn món trong nhật ký hoặc chọn ảnh trước nha"
                }

                if (_error.value == null) {
                    createdOwnerId?.let(::setCurrentUserId)
                    closeCreateSheet()
                    resetPage()
                    refreshVisibleFeedAfterCreate()
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

    private fun syncCurrentUserId() {
        setCurrentUserId(feedRepository.currentUserId())
    }

    private fun setCurrentUserId(currentUserId: String) {
        if (_currentUserId.value != currentUserId) {
            _selectedPostId.value = null
            _selectedPostIndex.value = -1
            _currentUserId.value = currentUserId
        }
    }

    private suspend fun refreshVisibleFeedAfterCreate() {
        refreshVisibleFeedSilently("[FeedVM] Silent feed refresh after create failed")
    }

    private fun subscribeToRealtimePosts() {
        feedRepository.subscribeToFeedRealtime()
        viewModelScope.launch {
            feedRepository.postRealtimeEvents.collect {
                scheduleRealtimeRefresh()
            }
        }
    }

    private fun subscribeToRealtimeFriendships() {
        friendRepository.subscribeToFriendshipRealtime()
        viewModelScope.launch {
            friendRepository.friendshipRealtimeEvents.collect {
                scheduleRealtimeRefresh()
            }
        }
    }

    private fun scheduleRealtimeRefresh() {
        realtimeRefreshJob?.cancel()
        realtimeRefreshJob = viewModelScope.launch {
            delay(500)
            refreshVisibleFeedSilently("[FeedVM] Realtime feed refresh failed")
        }
    }

    private fun startAutoRefresh() {
        if (autoRefreshJob != null) return

        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                if (canAutoRefreshFeed()) {
                    refreshVisibleFeedSilently("[FeedVM] Auto feed refresh failed")
                }
            }
        }
    }

    private fun canAutoRefreshFeed(): Boolean =
        !_isLoading.value &&
            !_isCreatingPost.value &&
            !_isCreateSheetOpen.value

    private suspend fun refreshVisibleFeedSilently(errorLogMessage: String) {
        runCatching {
            friendRepository.refreshCurrentProfile().getOrThrow()
            friendRepository.refreshFriendships().getOrThrow()
            syncCurrentUserId()
            feedRepository.refreshVisibleFromSupabase()
        }.onFailure { throwable ->
            Timber.e(throwable, errorLogMessage)
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            runCatching { feedRepository.toggleLike(postId) }
                .onSuccess {
                    SyncScheduler.triggerImmediateSync(context)
                    refreshVisibleFeedSilently("[FeedVM] Refresh after like failed")
                }
                .onFailure { throwable ->
                    Timber.e(throwable, "[FeedVM] Toggle like failed")
                    _error.value = throwable.message ?: "Không cập nhật được lượt thích"
                }
        }
    }

    fun hidePost(postId: String) {
        viewModelScope.launch {
            runCatching { feedRepository.hidePost(postId) }
                .onSuccess {
                    closePostDetail()
                    refreshVisibleFeedSilently("[FeedVM] Refresh after post hide failed")
                }
                .onFailure { throwable ->
                    Timber.e(throwable, "[FeedVM] Hide post failed")
                    _error.value = throwable.message ?: "Không ẩn được bài viết"
                }
        }
    }

    fun downloadPostImage(post: FeedPostDto) {
        viewModelScope.launch {
            runCatching { savePostImageToGallery(post) }
                .onSuccess {
                    _error.value = "Đã tải ảnh về thư viện"
                }
                .onFailure { throwable ->
                    Timber.e(throwable, "[FeedVM] Download post image failed")
                    _error.value = throwable.message ?: "Không tải được ảnh"
                }
        }
    }

    fun addComment(
        postId: String,
        body: String,
        parentCommentId: String? = null
    ) {
        if (body.isBlank()) return
        viewModelScope.launch {
            runCatching { feedRepository.addComment(postId, body, parentCommentId) }
                .onSuccess { SyncScheduler.triggerImmediateSync(context) }
                .onFailure { throwable ->
                    Timber.e(throwable, "[FeedVM] Add comment failed")
                    _error.value = throwable.message ?: "Không gửi được bình luận"
                }
        }
    }

    fun editComment(commentId: String, body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            runCatching { feedRepository.editComment(commentId, body) }
                .onSuccess {
                    SyncScheduler.triggerImmediateSync(context)
                    refreshVisibleFeedSilently("[FeedVM] Refresh after comment edit failed")
                }
                .onFailure { throwable ->
                    Timber.e(throwable, "[FeedVM] Edit comment failed")
                    _error.value = throwable.message ?: "Không sửa được bình luận"
                }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            runCatching { feedRepository.deleteComment(commentId) }
                .onSuccess {
                    SyncScheduler.triggerImmediateSync(context)
                    refreshVisibleFeedSilently("[FeedVM] Refresh after comment delete failed")
                }
                .onFailure { throwable ->
                    Timber.e(throwable, "[FeedVM] Delete comment failed")
                    _error.value = throwable.message ?: "Không xóa được bình luận"
                }
        }
    }

    fun setCommentHidden(commentId: String, isHidden: Boolean) {
        viewModelScope.launch {
            runCatching { feedRepository.setCommentHidden(commentId, isHidden) }
                .onSuccess {
                    SyncScheduler.triggerImmediateSync(context)
                    refreshVisibleFeedSilently("[FeedVM] Refresh after comment visibility failed")
                }
                .onFailure { throwable ->
                    Timber.e(throwable, "[FeedVM] Toggle comment visibility failed")
                    _error.value = throwable.message ?: "KhÃ´ng cáº­p nháº­t Ä‘Æ°á»£c bÃ¬nh luáº­n"
                }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            runCatching { feedRepository.deletePost(postId) }
                .onSuccess { remoteSynced ->
                    closePostDetail()
                    if (!remoteSynced) {
                        _error.value = "Đã ẩn bài viết, sẽ thử đồng bộ xóa lại khi có mạng"
                        SyncScheduler.triggerImmediateSync(context)
                    }
                }
                .onFailure { throwable ->
                    Timber.e(throwable, "[FeedVM] Delete post failed")
                    if (throwable is FeedPostDeleteSyncException) {
                        closePostDetail()
                        SyncScheduler.triggerImmediateSync(context)
                    }
                    _error.value = throwable.message ?: "Không xóa được bài viết"
                }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun showError(message: String) {
        _error.value = message
    }

    private fun clearDraft() {
        _selectedSourceItem.value = null
        _pickedImageUri.value = null
        _draftFreeImageTitle.value = ""
        _draftCaption.value = ""
        _draftVisibility.value = FeedVisibility.FRIENDS.value
    }

    private suspend fun copyPickedImageToFeedStorage(uri: Uri): String =
        withContext(Dispatchers.IO) {
            val sourceBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Khong doc duoc anh da chon")
            val shouldCompress = sourceBytes.size > MaxFeedPostImageBytes ||
                context.readImageMaxSide(uri, sourceBytes) > FeedPostImageMaxSide
            val outputBytes = if (shouldCompress) {
                context.compressPickedFeedImage(uri, sourceBytes)
            } else {
                sourceBytes
            }
            val extension = if (shouldCompress) {
                "jpg"
            } else {
                context.contentResolver.getType(uri)
                    ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                    ?: "jpg"
            }
            val directory = File(context.filesDir, "feed_posts").apply { mkdirs() }
            val target = File(directory, "${UUID.randomUUID()}.$extension")

            context.contentResolver.openInputStream(uri)?.use {
                target.outputStream().use { output -> output.write(outputBytes) }
            } ?: error("Không đọc được ảnh đã chọn")

            Uri.fromFile(target).toString()
        }

    private fun Context.readImageMaxSide(uri: Uri, bytes: ByteArray): Int {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        val rotated = contentResolver.openInputStream(uri)?.use { input ->
            val orientation = ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                orientation == ExifInterface.ORIENTATION_ROTATE_270
        } ?: false

        return if (rotated) max(options.outHeight, options.outWidth) else max(options.outWidth, options.outHeight)
    }

    private fun Context.compressPickedFeedImage(uri: Uri, sourceBytes: ByteArray): ByteArray {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, boundsOptions)

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateFeedInSampleSize(boundsOptions, MaxFeedPostDecodeSize)
        }
        var decoded = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, decodeOptions)
            ?: error("Anh khong hop le")

        val rotationDegrees = contentResolver.openInputStream(uri)?.use { input ->
            val orientation = ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        if (rotationDegrees != 0f) {
            val rotated = Bitmap.createBitmap(
                decoded,
                0,
                0,
                decoded.width,
                decoded.height,
                Matrix().apply { postRotate(rotationDegrees) },
                true
            )
            if (rotated != decoded) {
                decoded.recycle()
                decoded = rotated
            }
        }

        val maxSide = max(decoded.width, decoded.height)
        var output = if (maxSide > FeedPostImageMaxSide) {
            val scale = FeedPostImageMaxSide / maxSide.toFloat()
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).roundToInt().coerceAtLeast(1),
                (decoded.height * scale).roundToInt().coerceAtLeast(1),
                true
            ).also {
                if (it != decoded) decoded.recycle()
            }
        } else {
            decoded
        }

        var lastBytes = ByteArray(0)
        while (true) {
            var quality = FeedPostImageQuality
            while (quality >= MinFeedPostImageQuality) {
                val stream = ByteArrayOutputStream()
                output.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                lastBytes = stream.toByteArray()
                if (lastBytes.size <= MaxFeedPostImageBytes) {
                    output.recycle()
                    return lastBytes
                }
                quality -= 5
            }

            val currentMaxSide = max(output.width, output.height)
            if (currentMaxSide <= 720) {
                output.recycle()
                return lastBytes
            }

            val resized = Bitmap.createScaledBitmap(
                output,
                (output.width * 0.85f).roundToInt().coerceAtLeast(1),
                (output.height * 0.85f).roundToInt().coerceAtLeast(1),
                true
            )
            output.recycle()
            output = resized
        }
    }

    private fun calculateFeedInSampleSize(
        options: BitmapFactory.Options,
        maxSize: Int
    ): Int {
        var inSampleSize = 1
        val halfWidth = options.outWidth / 2
        val halfHeight = options.outHeight / 2

        while (halfWidth / inSampleSize >= maxSize || halfHeight / inSampleSize >= maxSize) {
            inSampleSize *= 2
        }

        return inSampleSize
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
        const val AUTO_REFRESH_INTERVAL_MS = 5_000L
    }
}
