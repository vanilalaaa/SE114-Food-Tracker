package com.SE114.food_tracker.feature.diary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.sync.SyncScheduler
import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.entities.Item
import com.SE114.food_tracker.data.local.entities.SyncStatus
import com.SE114.food_tracker.data.repository.ImageRepository
import com.SE114.food_tracker.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val categoryDAO: CategoryDAO,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedDate       = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _isLoading          = MutableStateFlow(false)
    private val _error              = MutableStateFlow<String?>(null)
    private val _streak             = MutableStateFlow(0)
    private val _mutationTrigger    = MutableStateFlow(0)

    // ── Pending image state ───────────────────────────────────────────────────
    // Holds the local URI so the UI can show a preview before the item is saved.
    // Cleared after saveItem() consumes it.
    private val _pendingImageUri   = MutableStateFlow<Uri?>(null)
    val pendingImageUri: StateFlow<Uri?> = _pendingImageUri.asStateFlow()

    // Raw compressed bytes — not exposed to UI, only consumed by saveItem().
    private var _pendingImageBytes: ByteArray? = null

    private val categories: Flow<List<DiaryCategory>> =
        categoryDAO.getVisibleCategories()
            .map { list ->
                list.map { category ->
                    DiaryCategory(
                        categoryId = category.categoryId,
                        name       = category.name,
                        iconUrl    = category.iconUrl,
                        isHidden   = category.isHidden,
                        isSystem   = category.isSystem
                    )
                }
            }

    private val selectedDayItems: Flow<List<DiaryItem>> =
        combine(_selectedDate, _mutationTrigger) { date, _ -> date }
            .flatMapLatest { date ->
                val range = date.utcDayRange()
                itemRepository.getDiaryItemsByDay(range.start, range.end)
            }

    private val datesWithData: Flow<Set<Int>> =
        combine(_selectedDate, _mutationTrigger) { date, _ -> date }
            .flatMapLatest { date ->
                val monthStart    = LocalDate(date.year, date.monthNumber, 1)
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
            categories,
            datesWithData
        ) { selectedDate, selectedCategoryId, items, categories, datesWithData ->
            DiaryContent(
                selectedDate       = selectedDate,
                selectedCategoryId = selectedCategoryId,
                items              = items,
                categories         = categories,
                datesWithData      = datesWithData
            )
        }.let { content ->
            combine(content, _isLoading, _error, _streak) { diaryContent, isLoading, error, streak ->
                val filteredItems = diaryContent.selectedCategoryId?.let { catId ->
                    diaryContent.items.filter { it.categoryId == catId }
                } ?: diaryContent.items

                DiaryUiState(
                    items              = diaryContent.items,
                    categories         = diaryContent.categories,
                    selectedDate       = diaryContent.selectedDate,
                    selectedCategoryId = diaryContent.selectedCategoryId,
                    datesWithData      = diaryContent.datesWithData,
                    totalSpend         = filteredItems.sumOf { it.price },
                    itemCount          = filteredItems.size,
                    streak             = streak,
                    isLoading          = isLoading,
                    error              = error
                )
            }
        }
            .catch { throwable ->
                _error.value = throwable.message
                emit(DiaryUiState(isLoading = false, error = throwable.message))
            }
            .stateIn(
                scope          = viewModelScope,
                started        = SharingStarted.WhileSubscribed(5000),
                initialValue   = DiaryUiState(isLoading = true)
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

    /**
     * Called by DiaryScreen after TakePicture / PickVisualMedia returns a Uri.
     * Reads bytes off the main thread is fine here because we're already on a
     * coroutine (IO) context; however, we do the work in a launch so the caller
     * doesn't need to be a suspend site.
     *
     * Compression: decodes the bitmap then re-encodes at 80 % JPEG quality.
     * Images larger than 1 MB after compression are still uploaded as-is — Storage
     * can handle it; the limit only matters for mobile data cost.
     */
    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                // 1. Đọc luồng byte chạy trên luồng IO để tránh Block Main Thread
                val rawBytes = withContext(Dispatchers.IO) {
                    context.contentResolver
                        .openInputStream(uri)
                        ?.use { it.readBytes() }
                } ?: return@runCatching

                // 2. Xử lý nén ảnh nặng trên luồng Default (CPU Bound)
                val compressed = compressToJpeg(rawBytes, maxBytes = 1_024 * 1_024)

                _pendingImageBytes = compressed
                _pendingImageUri.value = uri
                Timber.d("[DiaryVM] image selected, compressed to ${compressed.size / 1024} KB")
            }.onFailure { e ->
                Timber.e(e, "[DiaryVM] failed to read image")
                _error.value = "Không đọc được ảnh: ${e.message}"
            }
        }
    }

    /** Clears the pending image — call when the user dismisses FoodEntryScreen without saving. */
    fun clearPendingImage() {
        _pendingImageUri.value = null
        _pendingImageBytes     = null
    }

    fun saveItem(
        name: String,
        price: Double,
        categoryId: String,
        rating: Int,
        note: String,
        timeType: Int
    ) {
        viewModelScope.launch {
            runMutation {
                val now = Clock.System.now().toEpochMilliseconds()

                // 1. Insert to Room first — this gives us a stable itemId.
                val item = Item(
                    categoryId = categoryId,
                    name       = name,
                    timeType   = timeType,
                    price      = price,
                    rating     = rating.takeIf { it > 0 },
                    note       = note.ifBlank { null },
                    syncStatus = SyncStatus.PENDING.name,
                    entryDate  = _selectedDate.value.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
                    createdAt  = now,
                    updatedAt  = now
                )
                itemRepository.insert(item)

                // 2. If image bytes are waiting, upload now and patch the imageUrl.
                val bytes = _pendingImageBytes
                if (bytes != null) {
                    uploadImageAndPatchItem(item.itemId, bytes)
                }
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
        timeType: Int
    ) {
        viewModelScope.launch {
            runMutation {
                val currentItem = itemRepository.getItemById(itemId).first()
                if (currentItem == null) {
                    _error.value = "Không tìm thấy món ăn"
                    return@runMutation
                }
                val updated = currentItem.copy(
                    categoryId = categoryId,
                    name       = name,
                    timeType   = timeType,
                    price      = price,
                    rating     = rating.takeIf { it > 0 },
                    note       = note.ifBlank { null },
                    syncStatus = SyncStatus.PENDING.name,
                    updatedAt  = Clock.System.now().toEpochMilliseconds()
                )
                itemRepository.update(updated)

                // If a new image was selected for an existing item, upload and patch.
                val bytes = _pendingImageBytes
                if (bytes != null) {
                    uploadImageAndPatchItem(itemId, bytes)
                }
            }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            runMutation {
                itemRepository.softDeleteDiaryItem(itemId)
            }
        }
    }

    suspend fun computeStreak(): Int {
        var cursor = Clock.System.todayIn(TimeZone.currentSystemDefault())
        var streak = 0
        while (true) {
            val range = cursor.utcDayRange()
            val count = itemRepository.getItemCountForDay(range.start, range.end).first()
            if (count <= 0) break
            streak += 1
            cursor  = cursor.plus(DatePeriod(days = -1))
        }
        _streak.value = streak
        return streak
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Uploads [bytes] to Supabase Storage under items/<ownerId>/<itemId>.jpg,
     * then patches the local Room row with the returned public URL and marks
     * it PENDING again so the Sync worker will push the updated imageUrl.
     * Clears _pendingImageBytes/_pendingImageUri on success.
     */
    private suspend fun uploadImageAndPatchItem(itemId: String, bytes: ByteArray) {
        val ownerId = itemRepository.getCurrentUserId()
        if (ownerId == null) {
            Timber.w("[DiaryVM] no auth session — skipping image upload for item $itemId")
            clearPendingImage()
            return
        }

        imageRepository.uploadItemImage(ownerId, itemId, bytes)
            .onSuccess { publicUrl ->
                Timber.d("[DiaryVM] image uploaded → $publicUrl")
                val storedItem = itemRepository.getItemById(itemId).first()
                if (storedItem != null) {
                    itemRepository.update(
                        storedItem.copy(
                            imageUrl   = publicUrl,
                            syncStatus = SyncStatus.PENDING.name,
                            updatedAt  = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                }
                clearPendingImage()
            }
            .onFailure { e ->
                Timber.e(e, "[DiaryVM] image upload failed for item $itemId — item saved without image")
                // Don't surface as a hard error; the item is already saved.
                // Image upload can be retried later (Sprint 2 TODO).
                clearPendingImage()
            }
    }

    /**
     * Compresses [rawBytes] as JPEG at 80 % quality.
     * Chuyển thành hàm suspend và ép chạy trên Dispatchers.Default
     */
    private suspend fun compressToJpeg(rawBytes: ByteArray, maxBytes: Int): ByteArray =
        withContext(Dispatchers.Default) {
            val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                ?: return@withContext rawBytes // không decode được — trả về mảng gốc

            for (quality in listOf(80, 60, 40)) {
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                val compressed = out.toByteArray()
                if (compressed.size <= maxBytes || quality == 40) return@withContext compressed
            }
            return@withContext rawBytes
        }

    private suspend fun runMutation(block: suspend () -> Unit) {
        _isLoading.value = true
        _error.value     = null
        try {
            block()
            computeStreak()
            _mutationTrigger.value += 1
            SyncScheduler.triggerImmediateSync(context)
        } catch (t: Throwable) {
            _error.value = t.message
        } finally {
            _isLoading.value = false
        }
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
        val categories: List<DiaryCategory>,
        val datesWithData: Set<Int>
    )
}