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
    private val _pendingImageUri = MutableStateFlow<Uri?>(null)
    val pendingImageUri: StateFlow<Uri?> = _pendingImageUri.asStateFlow()

    // Compressed bytes waiting to be uploaded. Written by onImageSelected(),
    // read by saveItem()/updateItem() after joining _imageCompressionJob.
    private var _pendingImageBytes: ByteArray? = null

    // Handle to the compression coroutine so save/update can await it before
    // reading _pendingImageBytes — prevents the bytes-still-null race.
    private var _imageCompressionJob: Job? = null

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
            combine(content, _isLoading, _error, _streak, _pendingImageUri) {
                    diaryContent, isLoading, error, streak, pendingImageUri ->
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
        // Show thumbnail immediately; clear any stale bytes from a previous selection.
        _pendingImageUri.value = uri
        _pendingImageBytes     = null

        // Save the Job so saveItem/updateItem can join() it before reading bytes.
        _imageCompressionJob = viewModelScope.launch {
            runCatching {
                val rawBytes = withContext(Dispatchers.IO) {
                    context.contentResolver
                        .openInputStream(uri)
                        ?.use { it.readBytes() }
                } ?: return@runCatching

                val compressed = compressToJpeg(rawBytes, maxBytes = 1_024 * 1_024)
                _pendingImageBytes = compressed
                Timber.d("[DiaryVM] image ready, compressed to ${compressed.size / 1024} KB")
            }.onFailure { e ->
                Timber.e(e, "[DiaryVM] failed to read/compress image")
                _pendingImageBytes     = null
                _pendingImageUri.value = null
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
        timeType: Int
    ) {
        viewModelScope.launch {
            // Wait for compression to finish so imageBytes is never null when an
            // image was selected.
            _imageCompressionJob?.join()
            val imageBytes = _pendingImageBytes

            // ── STEP 1: save the item to Room and sync it (without imageUrl yet
            //           if an image is attached — that comes in Step 2).
            // runMutation finishes → triggerImmediateSync fires → Sync worker upserts
            // the item row (image_url still null at this point, which is fine).
            val now = Clock.System.now().toEpochMilliseconds()
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

            runMutation {
                itemRepository.insert(item)
            }

            // ── STEP 2: upload the image AFTER runMutation (and its sync) returns.
            // uploadImageAndPatchItem writes imageUrl back to Room then calls
            // triggerImmediateSync itself, so the second sync carries the real URL.
            if (imageBytes != null) {
                uploadImageAndPatchItem(item.itemId, imageBytes)
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
            _imageCompressionJob?.join()
            val imageBytes = _pendingImageBytes

            // ── STEP 1: update the item fields in Room and sync.
            runMutation {
                val currentItem = itemRepository.getItemByIdOneShot(itemId)
                if (currentItem == null) {
                    Timber.e("[DiaryVM] updateItem: item $itemId not found in Room")
                    _error.value = "Không tìm thấy món ăn"
                    return@runMutation
                }
                itemRepository.update(
                    currentItem.copy(
                        categoryId = categoryId,
                        name       = name,
                        timeType   = timeType,
                        price      = price,
                        rating     = rating.takeIf { it > 0 },
                        note       = note.ifBlank { null }
                    )
                )
            }

            // ── STEP 2: upload image and patch imageUrl with its own sync trigger.
            if (imageBytes != null) {
                uploadImageAndPatchItem(itemId, imageBytes)
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

                val storedItem = itemRepository.getItemByIdOneShot(itemId)
                if (storedItem != null) {
                    // Write imageUrl into Room. ItemRepository.update() marks the
                    // item PENDING again, so the next sync will carry the real URL.
                    itemRepository.update(storedItem.copy(imageUrl = publicUrl))
                    Timber.d("[DiaryVM] imageUrl patched in Room for item $itemId")

                    // FIX: trigger a dedicated sync NOW so the patched imageUrl
                    // reaches Supabase. Without this second trigger the item stays
                    // PENDING in Room but no worker ever picks it up.
                    SyncScheduler.triggerImmediateSync(context)
                    Timber.d("[DiaryVM] immediate sync triggered after imageUrl patch")
                } else {
                    Timber.e("[DiaryVM] item $itemId disappeared from Room before imageUrl patch")
                }
                clearPendingImage()
            }
            .onFailure { e ->
                Timber.e(e, "[DiaryVM] image upload failed for item $itemId — saved without image")
                clearPendingImage()
            }
    }

    private suspend fun compressToJpeg(rawBytes: ByteArray, maxBytes: Int): ByteArray =
        withContext(Dispatchers.Default) {
            val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                ?: return@withContext rawBytes

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
            Timber.e(t, "[DiaryVM] mutation failed")
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