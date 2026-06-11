package com.SE114.food_tracker.feature.diary

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SE114.food_tracker.core.designsystem.components.DiaryTopBar
import com.SE114.food_tracker.core.designsystem.components.NutritionCard
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.MainBackground
import com.SE114.food_tracker.feature.diary.components.CalendarCard
import com.SE114.food_tracker.feature.diary.components.CategoryRowItem
import kotlinx.datetime.LocalDate
import com.SE114.food_tracker.feature.diary.components.MonthYearPickerDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Surface

@Composable
fun DiaryScreen(
    triggerAdd: Boolean = false,
    onAddTriggered: () -> Unit = {}
) {
    val diaryViewModel    = hiltViewModel<DiaryViewModel>()
    val categoryViewModel = hiltViewModel<CategoryViewModel>()
    val uiState      by diaryViewModel.uiState.collectAsStateWithLifecycle()
    val categoryState by categoryViewModel.visibleCategories.collectAsStateWithLifecycle()

    val categories = categoryState.ifEmpty { uiState.categories }

    DiaryScreenContent(
        uiState = uiState,
        categories = categories,
        triggerAdd = triggerAdd,
        onAddTriggered = onAddTriggered,
        onLoadDate = { diaryViewModel.loadDate(it) },
        onSaveItem = { n, p, c, r, no, t -> diaryViewModel.saveItem(n, p, c, r, no, t) },
        onUpdateItem = { id, n, p, c, r, no, t -> diaryViewModel.updateItem(id, n, p, c, r, no, t) },
        onDeleteItem = { diaryViewModel.deleteItem(it) },
        onDeleteCategory = { categoryViewModel.deleteCategory(it) },
        onToggleCategoryVisibility = { categoryViewModel.toggleVisibility(it) },
        onSelectCategoryFilter = { catId -> diaryViewModel.selectCategoryFilter(catId) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreenContent(
    uiState: DiaryUiState,
    categories: List<DiaryCategory>,
    triggerAdd: Boolean,
    onAddTriggered: () -> Unit,
    onLoadDate: (LocalDate) -> Unit,
    onSaveItem: (String, Double, String, Int, String, Int) -> Unit,
    onUpdateItem: (String, String, Double, String, Int, String, Int) -> Unit,
    onDeleteItem: (String) -> Unit,
    onDeleteCategory: (DiaryCategory) -> Unit,
    onToggleCategoryVisibility: (DiaryCategory) -> Unit,
    onSelectCategoryFilter: (String?) -> Unit
) {
    var showDetailSheet     by remember { mutableStateOf(false) }
    var showEntryScreen     by remember { mutableStateOf(false) }
    var showSourceScreen    by remember { mutableStateOf(false) }
    var selectedItemForEdit by remember { mutableStateOf<DiaryItem?>(null) }
    var showManageCategories by remember { mutableStateOf(false) }
    var showDatePicker       by remember { mutableStateOf(false) }
    var stickerScale by remember { mutableStateOf(1f) }
    var boxScale by remember { mutableStateOf(1f) }
    var calendarScale by remember { mutableStateOf(1f) }
    val scrollState = rememberScrollState()
    var preSelectedCategory by remember { mutableStateOf<DiaryCategory?>(null) }

    val filteredItems = remember(uiState.items, uiState.selectedCategoryId) {
        uiState.selectedCategoryId?.let { catId ->
            uiState.items.filter { it.categoryId == catId }
        } ?: uiState.items
    }

    LaunchedEffect(triggerAdd) {
        if (triggerAdd) {
            selectedItemForEdit = null
            showSourceScreen = true
            onAddTriggered()
        }
    }

    if (showDatePicker) {
        MonthYearPickerDialog(
            currentMonth = uiState.selectedMonth,
            currentYear = uiState.selectedYear,
            onDismiss = { showDatePicker = false },
            onConfirm = { month, year ->
                runCatching { LocalDate(year, month, 1) }
                    .getOrNull()
                    ?.let { onLoadDate(it) }
                showDatePicker = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .verticalScroll(scrollState)
    ) {
        DiaryTopBar(
            streakCount  = uiState.streak.toString(),
            currentMonth = "Tháng ${uiState.selectedMonth} ${uiState.selectedYear}",
            onMonthClick = { showDatePicker = true },
            onPreviousClick = {
                val newMonth = if (uiState.selectedMonth == 1) 12 else uiState.selectedMonth - 1
                val newYear = if (uiState.selectedMonth == 1) uiState.selectedYear - 1 else uiState.selectedYear
                runCatching { LocalDate(newYear, newMonth, 1) }.getOrNull()?.let { onLoadDate(it) }
            },
            onNextClick = {
                val newMonth = if (uiState.selectedMonth == 12) 1 else uiState.selectedMonth + 1
                val newYear = if (uiState.selectedMonth == 12) uiState.selectedYear + 1 else uiState.selectedYear
                runCatching { LocalDate(newYear, newMonth, 1) }.getOrNull()?.let { onLoadDate(it) }
            }
        )
        NutritionCard(
            unfilteredItems = uiState.items,
            filteredItemCount = filteredItems.size,
            categories = categories,
            selectedCategoryId = uiState.selectedCategoryId,
            onCategorySelect = onSelectCategoryFilter,
            boxScale = boxScale,
            calendarScale = calendarScale,
            onBoxScaleChange = { boxScale = it },
            onCalendarScaleChange = { calendarScale = it }
        )

        Spacer(modifier = Modifier.height(16.dp))
        CalendarCard(
            selectedYear  = uiState.selectedYear,
            selectedMonth = uiState.selectedMonth,
            onDateClick = { day ->
                runCatching {
                    LocalDate(uiState.selectedYear, uiState.selectedMonth, day)
                }.getOrNull()?.let { selectedDate ->
                    onLoadDate(selectedDate)
                    showDetailSheet = true
                }
            },
            hasDataDates = uiState.datesWithData.toList(),
            scale = calendarScale
        )
        Spacer(modifier = Modifier.height(32.dp))
    }


    if (showDetailSheet) {
        DayDetailBottomSheet(
            onDismiss    = { showDetailSheet = false },
            selectedDate = uiState.selectedDate,
            items        = uiState.filteredItems,
            categories   = categories,
            onEditItem   = { item ->
                selectedItemForEdit = item
                showDetailSheet     = false
                showEntryScreen     = true
            },
            onAddItem    = {
                selectedItemForEdit = null
                showDetailSheet     = false
                showSourceScreen    = true
            },
            onDeleteItem = onDeleteItem
        )
    }

    if (showSourceScreen) {
        Dialog(
            onDismissRequest = { showSourceScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            AddFoodSourceScreen(
                categories = categories,
                onBack = { showSourceScreen = false },
                onCameraSelected = {
                    showSourceScreen = false
                    preSelectedCategory = null
                    showEntryScreen = true
                },
                onGallerySelected = {
                    showSourceScreen = false
                    preSelectedCategory = null
                    showEntryScreen = true
                },
                onPresetSelected = { category ->
                    preSelectedCategory = category
                },
                onManualSelected = {
                    showSourceScreen = false
                    showEntryScreen = true
                }
            )
        }
    }

    if (showEntryScreen) {
        Dialog(
            onDismissRequest = {
                showEntryScreen = false
                selectedItemForEdit = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false) // ÉP FULL MÀN HÌNH
        ) {
            // Bọc Surface để cản lại mọi thứ từ màn hình dưới
            Surface(modifier = Modifier.fillMaxSize(), color = MainBackground) {
                val editingItem = selectedItemForEdit
                FoodEntryScreen(
                    existingItem = editingItem,
                    preSelectedCategory = preSelectedCategory,
                    categories   = categories,
                    onDismiss    = {
                        showEntryScreen     = false
                        selectedItemForEdit = null
                        preSelectedCategory = null
                    },
                    onSave = { name, price, categoryId, rating, note, timeType ->
                        if (editingItem == null) {
                            onSaveItem(name, price, categoryId, rating, note, timeType)
                        } else {
                            onUpdateItem(editingItem.itemId, name, price, categoryId, rating, note, timeType)
                        }
                        showEntryScreen     = false
                        selectedItemForEdit = null
                        showDetailSheet     = true
                        preSelectedCategory = null
                    },
                    onDelete = { itemId ->
                        onDeleteItem(itemId)
                        showEntryScreen     = false
                        selectedItemForEdit = null
                    },
                    onManageCategories = { showManageCategories = true }
                )
            }
        }
    }

    if (showManageCategories) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showManageCategories = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Quản lý danh mục",
                    modifier = Modifier.padding(vertical = 16.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                )
                LazyColumn {
                    items(
                        items = categories,
                        key   = { it.categoryId }
                    ) { category ->
                        CategoryRowItem(
                            category = category,
                            onEdit   = { },
                            onDelete = { onDeleteCategory(category) },
                            onVisibilityToggle = { onToggleCategoryVisibility(category) }
                        )
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "＋ Thêm danh mục mới (Sprint 2)",
                            modifier = Modifier.padding(12.dp),
                            color = androidx.compose.ui.graphics.Color(0xFFC98989)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DiaryScreenPreview() {
    FoodTrackerTheme {
        DiaryScreenContent(
            uiState = DiaryUiState(
                streak = 5,
                datesWithData = setOf(4, 5, 6),
                totalSpend = 45000.0,
                itemCount = 2
            ),
            categories = listOf(
                DiaryCategory("1", "Cơm", "🍚", isSystem = true),
                DiaryCategory("2", "Mì & Phở", "🍜", isSystem = true)
            ),
            triggerAdd = false,
            onAddTriggered = {},
            onLoadDate = {},
            onSaveItem = { _, _, _, _, _, _ -> },
            onUpdateItem = { _, _, _, _, _, _, _ -> },
            onDeleteItem = {},
            onDeleteCategory = {},
            onToggleCategoryVisibility = {},
            onSelectCategoryFilter = {}
        )
    }
}