package com.SE114.food_tracker.feature.diary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.sync.SyncScheduler
import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.entities.Item
import com.SE114.food_tracker.core.sync.SyncStatus
import com.SE114.food_tracker.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryDAO: CategoryDAO,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedDate      = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _isLoading         = MutableStateFlow(false)
    private val _error             = MutableStateFlow<String?>(null)
    private val _streak            = MutableStateFlow(0)
    private val _mutationTrigger   = MutableStateFlow(0)

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
                itemRepository.insert(
                    Item(
                        categoryId   = categoryId,
                        name         = name,
                        timeType     = timeType,
                        price        = price,
                        rating       = rating.takeIf { it > 0 },
                        note         = note.ifBlank { null },
                        syncStatus   = SyncStatus.PENDING.name,
                        entryDate    = _selectedDate.value.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
                        createdAt    = now,
                        updatedAt    = now
                    )
                )
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
                itemRepository.update(
                    currentItem.copy(
                        categoryId = categoryId,
                        name       = name,
                        timeType   = timeType,
                        price      = price,
                        rating     = rating.takeIf { it > 0 },
                        note       = note.ifBlank { null },
                        syncStatus = SyncStatus.PENDING.name,
                        updatedAt  = Clock.System.now().toEpochMilliseconds()
                    )
                )
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