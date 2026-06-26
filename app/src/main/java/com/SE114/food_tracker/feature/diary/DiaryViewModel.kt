package com.SE114.food_tracker.feature.diary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.sync.SyncScheduler
import com.SE114.food_tracker.data.local.entities.Item
import com.SE114.food_tracker.core.sync.SyncStatus
import com.SE114.food_tracker.data.repository.ImageRepository
import com.SE114.food_tracker.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val imageRepository: ImageRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedDate       = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _isLoading          = MutableStateFlow(false)
    private val _error              = MutableStateFlow<String?>(null)
    private val _streak             = MutableStateFlow(0)

    // ── Trạng thái ảnh tạm thời ──────────────────────────────────────────────
    private val _pendingImageUri = MutableStateFlow<Uri?>(null)
    val pendingImageUri: StateFlow<Uri?> = _pendingImageUri.asStateFlow()

    private var _pendingImageBytes: ByteArray? = null
    private var _imageCompressionJob: Job? = null

    private val selectedDayItems: Flow<List<DiaryItem>> =
        _selectedDate.flatMapLatest { date ->
            val range = date.utcDayRange()
            itemRepository.getDiaryItemsByDay(range.start, range.end)
        }

    private val selectedMonthItems: Flow<List<DiaryItem>> =
        _selectedDate.flatMapLatest { date ->
            val monthStart     = LocalDate(date.year, date.monthNumber, 1)
            val nextMonthStart = monthStart.plus(DatePeriod(months = 1))
            itemRepository.getDiaryItemsByDay(
                monthStart.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
                nextMonthStart.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
            )
        }

    private val datesWithData: Flow<Set<Int>> =
        _selectedDate.flatMapLatest { date ->
            val monthStart     = LocalDate(date.year, date.monthNumber, 1)
            val nextMonthStart = monthStart.plus(DatePeriod(months = 1))
            val range = DateRange(
                start = monthStart.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
                end   = nextMonthStart.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
            )
            itemRepository.getItemsByDateRange(range.start, range.end)
                .map { items ->
                    items.map { item ->
                        Instant.fromEpochMilliseconds(item.entryDate)
                            .toLocalDateTime(TimeZone.UTC)
                            .date
                            .dayOfMonth
                    }.toSet()
                }
        }

    val uiState: StateFlow<DiaryUiState> =
        combine(
            _selectedDate,
            _selectedCategoryId,
            selectedDayItems,
            selectedMonthItems
        ) { selectedDate, selectedCategoryId, items, monthlyItems ->
            DiaryContent(
                selectedDate       = selectedDate,
                selectedCategoryId = selectedCategoryId,
                items              = items,
                monthlyItems       = monthlyItems,
                categories         = emptyList(),
                datesWithData      = emptySet()
            )
        }.combine(datesWithData) { content, datesWithData ->
            content.copy(datesWithData = datesWithData)
        }.let { content ->
            combine(content, _isLoading, _error, _streak, _pendingImageUri) {
                    diaryContent, isLoading, error, streak, pendingImageUri ->
                val filteredItems = diaryContent.selectedCategoryId?.let { catId ->
                    diaryContent.items.filter { it.categoryId == catId }
                } ?: diaryContent.items

                DiaryUiState(
                    items              = diaryContent.items,
                    monthlyItems       = diaryContent.monthlyItems,
                    categories         = diaryContent.categories,
                    selectedDate       = diaryContent.selectedDate,
                    selectedCategoryId = diaryContent.selectedCategoryId,
                    datesWithData      = diaryContent.datesWithData,
                    totalSpend         = filteredItems.sumOf { it.price },
                    itemCount          = filteredItems.size,
                    streak             = streak,
                    isLoading          = isLoading,
                    error              = error,
                    pendingImageUri    = pendingImageUri
                )
            }
        }
            .catch { throwable ->
                _error.value = throwable.message
                emit(DiaryUiState(isLoading = false, error = throwable.message))
            }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5000),
                initialValue = DiaryUiState(isLoading = true)
            )

    init {
        viewModelScope.launch { computeStreak() }
    }

    fun loadDate(date: LocalDate) {
        _selectedDate.value = date
        _error.value        = null
    }

    fun selectCategoryFilter(catId: String?) {
        _selectedCategoryId.value = catId
    }

    fun onImageSelected(uri: Uri) {
        _pendingImageUri.value = uri
        _pendingImageBytes     = null

        _imageCompressionJob = viewModelScope.launch {
            runCatching {
                val rawBytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: return@launch

                _pendingImageBytes = compressToJpeg(rawBytes, uri, maxBytes = 1_024 * 1_024)
                Timber.d("[DiaryVM] Ảnh đã chuẩn bị xong: ${_pendingImageBytes?.size?.div(1024)} KB")
            }.onFailure { e ->
                Timber.e(e, "[DiaryVM] Lỗi đọc/nén ảnh")
                clearPendingImage()
                _error.value = "Không đọc được ảnh: ${e.message}"
            }
        }
    }

    fun clearPendingImage() {
        _pendingImageUri.value = null
        _pendingImageBytes     = null
        _imageCompressionJob   = null
    }

    fun saveItem(
        name: String,
        price: Double,
        categoryId: String,
        rating: Int,
        note: String,
        timeType: Int,
        isShared: Boolean = false,
        pickedTimeMillis: Long = Clock.System.now().toEpochMilliseconds() // ← Thêm tham số nhận dạng thời gian thực ăn
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null

            try {
                _imageCompressionJob?.join()
                val imageBytes = _pendingImageBytes

                val itemId = java.util.UUID.randomUUID().toString()
                var finalImageUrl: String? = null

                imageBytes?.let { bytes ->
                    val ownerId = itemRepository.getCurrentUserId()
                    if (ownerId != null) {
                        imageRepository.uploadItemImage(ownerId, itemId, bytes)
                            .onSuccess { publicUrl ->
                                finalImageUrl = publicUrl
                                Timber.d("[DiaryVM] Upload ảnh thành công: $publicUrl")
                            }
                            .onFailure { e ->
                                Timber.e(e, "[DiaryVM] Lỗi upload ảnh Storage nhưng vẫn giữ thông tin chữ")
                            }
                    }
                }

                val now = Clock.System.now().toEpochMilliseconds()
                val item = Item(
                    itemId     = itemId,
                    categoryId = categoryId,
                    name       = name,
                    timeType   = timeType,
                    price      = price,
                    rating     = rating.takeIf { it > 0 },
                    note       = note.ifBlank { null },
                    imageUrl   = finalImageUrl,
                    isShared   = isShared,
                    syncStatus = SyncStatus.PENDING.name,
                    entryDate  = _selectedDate.value.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
                    createdAt  = pickedTimeMillis, // ← SỬA: Thay thế 'now' thành mốc thời gian người dùng chọn
                    updatedAt  = now
                )

                itemRepository.insert(item)
                clearPendingImage()
                computeStreak()
                SyncScheduler.triggerImmediateSync(context)

            } catch (t: Throwable) {
                _error.value = t.message
                Timber.e(t, "[DiaryVM] Thao tác lưu thất bại")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateItem(
        itemId: String,
        name: String,
        price: Double,
        categoryId: String,
        rating: Int,
        note: String,
        timeType: Int,
        isShared: Boolean,
        pickedTimeMillis: Long = Clock.System.now().toEpochMilliseconds()
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null

            try {
                _imageCompressionJob?.join()
                val imageBytes = _pendingImageBytes

                val currentItem = itemRepository.getItemByIdOneShot(itemId)
                if (currentItem == null) {
                    _error.value = "Không tìm thấy món ăn để cập nhật"
                    _isLoading.value = false
                    return@launch
                }

                var finalImageUrl = currentItem.imageUrl

                imageBytes?.let { bytes ->
                    val ownerId = itemRepository.getCurrentUserId()
                    if (ownerId != null) {
                        imageRepository.uploadItemImage(ownerId, itemId, bytes)
                            .onSuccess { publicUrl -> finalImageUrl = publicUrl }
                            .onFailure { e -> Timber.e(e, "[DiaryVM] Lỗi cập nhật ảnh mới") }
                    }
                }

                itemRepository.update(
                    currentItem.copy(
                        categoryId = categoryId,
                        name       = name,
                        timeType   = timeType,
                        price      = price,
                        rating     = rating.takeIf { it > 0 },
                        note       = note.ifBlank { null },
                        imageUrl   = finalImageUrl,
                        isShared   = isShared,
                        syncStatus = SyncStatus.PENDING.name,
                        createdAt  = pickedTimeMillis,
                        updatedAt  = Clock.System.now().toEpochMilliseconds()
                    )
                )
                clearPendingImage()
                computeStreak()
                SyncScheduler.triggerImmediateSync(context)

            } catch (t: Throwable) {
                _error.value = t.message
                Timber.e(t, "[DiaryVM] Thao tác cập nhật thất bại")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                itemRepository.softDeleteDiaryItem(itemId)
                computeStreak()
                SyncScheduler.triggerImmediateSync(context)
            } catch (t: Throwable) {
                _error.value = t.message
                Timber.e(t, "[DiaryVM] Xóa món thất bại")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun computeStreak(): Int = withContext(Dispatchers.IO) {
        try {
            val systemTimeZone = TimeZone.currentSystemDefault()
            val today = Clock.System.todayIn(systemTimeZone)

            val ownerId = itemRepository.getCurrentUserId().orEmpty()
            val distinctEpochs = itemRepository.getDistinctEntryDates(ownerId)

            val activeDates = distinctEpochs.map { epoch ->
                Instant.fromEpochMilliseconds(epoch)
                    .toLocalDateTime(systemTimeZone)
                    .date
            }.toSet()

            var streak = 0
            var cursor = today

            if (!activeDates.contains(cursor)) {
                cursor = cursor.plus(DatePeriod(days = -1))
            }

            while (activeDates.contains(cursor)) {
                streak++
                cursor = cursor.plus(DatePeriod(days = -1))
            }

            _streak.value = streak
            return@withContext streak
        } catch (e: Exception) {
            Timber.e(e, "[DiaryVM] Lỗi tính toán streak")
            return@withContext _streak.value
        }
    }

    private suspend fun compressToJpeg(rawBytes: ByteArray, uri: Uri, maxBytes: Int): ByteArray =
        withContext(Dispatchers.Default) {
            var bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                ?: return@withContext rawBytes

            runCatching {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val exif = ExifInterface(inputStream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )

                    val degrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }

                    if (degrees != 0f) {
                        val matrix = Matrix().apply { postRotate(degrees) }
                        val rotatedBitmap = Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                        )
                        if (rotatedBitmap != bitmap) {
                            bitmap.recycle()
                            bitmap = rotatedBitmap
                        }
                    }
                }
            }.onFailure { t ->
                Timber.e(t, "[DiaryVM] Lỗi đọc thông tin góc xoay EXIF")
            }

            for (quality in listOf(80, 60, 40)) {
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                val compressed = out.toByteArray()
                if (compressed.size <= maxBytes || quality == 40) return@withContext compressed
            }
            return@withContext rawBytes
        }

    private fun LocalDate.utcDayRange(): DateRange {
        val start = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        val end   = plus(DatePeriod(days = 1)).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        return DateRange(start = start, end = end)
    }

    private data class DateRange(val start: Long, val end: Long)

    private data class DiaryContent(
        val selectedDate: LocalDate,
        val selectedCategoryId: String?,
        val items: List<DiaryItem>,
        val monthlyItems: List<DiaryItem>,
        val categories: List<DiaryCategory>,
        val datesWithData: Set<Int>
    )
}