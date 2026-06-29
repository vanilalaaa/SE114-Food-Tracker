package com.SE114.food_tracker.feature.diary

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SE114.food_tracker.core.designsystem.components.*
import com.SE114.food_tracker.core.designsystem.components.DiaryTopBar
import com.SE114.food_tracker.core.designsystem.components.NutritionCard
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.MainBackground
import com.SE114.food_tracker.feature.diary.components.AddActionButton
import com.SE114.food_tracker.feature.diary.components.CalendarCard
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import com.SE114.food_tracker.data.repository.ChatRepository

private val DiaryAddButtonBottomPadding = BottomBarContentPadding + 10.dp
private val DiaryBottomContentPadding = BottomBarContentPadding + 80.dp

@Composable
fun DiaryScreen(
    triggerAdd: Boolean = false,
    onAddTriggered: () -> Unit = {}
) {
    val diaryViewModel    = hiltViewModel<DiaryViewModel>()
    val categoryViewModel = hiltViewModel<CategoryViewModel>()
    val uiState           by diaryViewModel.uiState.collectAsStateWithLifecycle()
    val categoryState     by categoryViewModel.visibleCategories.collectAsStateWithLifecycle()
    val allCategoryState  by categoryViewModel.allCategories.collectAsStateWithLifecycle()
    val pendingImageUri   by diaryViewModel.pendingImageUri.collectAsStateWithLifecycle()
    val categoryError     by categoryViewModel.error.collectAsStateWithLifecycle()

    // ── ĐÃ THÊM: Thu thập tín hiệu lính canh báo đã lưu từ ViewModel ──────────
    val itemSaved         by diaryViewModel.itemSaved.collectAsStateWithLifecycle()

    val categories       = categoryState.ifEmpty { uiState.categories }
    val manageCategories = allCategoryState.ifEmpty { categories }
    val availableWallets by diaryViewModel.availableWallets.collectAsStateWithLifecycle()

    DiaryScreenContent(
        uiState                    = uiState,
        pendingImageUri            = pendingImageUri,
        categories                 = categories,
        manageCategories           = manageCategories,
        availableWallets           = availableWallets,
        categoryDeleteError        = categoryError,
        triggerAdd                 = triggerAdd,
        itemSaved                  = itemSaved, // ← Truyền xuống Content
        onConsumeItemSaved         = { diaryViewModel.consumeItemSaved() }, // ← Truyền hàm reset
        onAddTriggered             = onAddTriggered,
        onClearPendingImage        = { diaryViewModel.clearPendingImage() },
        onClearCategoryError       = { categoryViewModel.clearError() },
        onLoadDate                 = { diaryViewModel.loadDate(it) },
        onImageSelected            = { diaryViewModel.onImageSelected(it) },
        onSaveItem                 = { n, p, c, r, no, t, shared, millis -> diaryViewModel.saveItem(n, p, c, r, no, t, shared, millis) },
        onUpdateItem               = { id, n, p, c, r, no, t, shared, millis -> diaryViewModel.updateItem(id, n, p, c, r, no, t, shared, millis) },
        onDeleteItem               = { diaryViewModel.deleteItem(it) },
        onDeleteCategory           = { categoryViewModel.deleteCategory(it) },
        onToggleCategoryVisibility = { categoryViewModel.toggleVisibility(it) },
        onEditCategory             = { category, newName, newIconUrl -> categoryViewModel.editCategory(category, newName, newIconUrl) },
        onCreateCategory           = { name, emoji -> categoryViewModel.addCategory(name, emoji) },
        onSelectCategoryFilter     = { catId -> diaryViewModel.selectCategoryFilter(catId) },
        onBoxScaleChange           = { diaryViewModel.updateBoxScale(it) },
        onCalendarScaleChange      = { diaryViewModel.updateCalendarScale(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreenContent(
    uiState: DiaryUiState,
    pendingImageUri: Uri?,
    categories: List<DiaryCategory>,
    manageCategories: List<DiaryCategory>,
    availableWallets: List<ChatRepository.WalletWithRole> = emptyList(),
    categoryDeleteError: String?,
    triggerAdd: Boolean,
    itemSaved: Boolean, // ← Đã nhận tín hiệu từ ViewModel
    onConsumeItemSaved: () -> Unit, // ← Đã nhận hàm clear tín hiệu
    onAddTriggered: () -> Unit,
    onClearCategoryError: () -> Unit,
    onLoadDate: (LocalDate) -> Unit,
    onImageSelected: (Uri) -> Unit,
    onClearPendingImage: () -> Unit,
    onSaveItem: (String, Double, String, Int, String, Int, Boolean, Long) -> Unit,
    onUpdateItem: (String, String, Double, String, Int, String, Int, Boolean, Long) -> Unit,
    onDeleteItem: (String) -> Unit,
    onDeleteCategory: (DiaryCategory) -> Unit,
    onToggleCategoryVisibility: (DiaryCategory) -> Unit,
    onEditCategory: (DiaryCategory, String, String) -> Unit,
    onCreateCategory: (String, String) -> Unit,
    onSelectCategoryFilter: (String?) -> Unit,
    onBoxScaleChange: (Float) -> Unit,
    onCalendarScaleChange: (Float) -> Unit
) {
    var showDetailSheet     by remember { mutableStateOf(false) }
    var showEntryScreen     by remember { mutableStateOf(false) }
    var showSourceScreen    by remember { mutableStateOf(false) }
    var selectedItemForEdit by remember { mutableStateOf<DiaryItem?>(null) }
    var showDatePicker      by remember { mutableStateOf(false) }
    val scrollState         = rememberScrollState()
    var preSelectedCategory by remember { mutableStateOf<DiaryCategory?>(null) }

    val sourceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val entrySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filteredItems = remember(uiState.items, uiState.selectedCategoryId) {
        uiState.selectedCategoryId?.let { catId ->
            uiState.items.filter { it.categoryId == catId }
        } ?: uiState.items
    }

    val filteredMonthlyItems = remember(uiState.monthlyItems, uiState.selectedCategoryId) {
        uiState.selectedCategoryId?.let { catId ->
            uiState.monthlyItems.filter { it.categoryId == catId }
        } ?: uiState.monthlyItems
    }

    // ── ĐÃ THÊM: Chờ đợi Room Flow hoàn thành ghi dữ liệu thực sự rồi mới bật BottomSheet ──
    LaunchedEffect(itemSaved) {
        if (itemSaved) {
            showDetailSheet = true // Chỉ mở sheet xem chi tiết khi DB đã lưu xong ổn định
            onConsumeItemSaved()   // Reset trạng thái lính canh về false
        }
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
            currentMonth = uiState.selectedDate.monthNumber,
            currentYear  = uiState.selectedDate.year,
            onDismiss    = { showDatePicker = false },
            onConfirm    = { month, year ->
                runCatching { LocalDate(year, month, 1) }
                    .getOrNull()
                    ?.let { onLoadDate(it) }
                showDatePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            DiaryTopBar(
                streakCount  = uiState.streak.toString(),
                currentMonth = "Tháng ${uiState.selectedDate.monthNumber} ${uiState.selectedDate.year}",
                onMonthClick = { showDatePicker = true },
                onPreviousClick = {
                    val previousMonthDate = uiState.selectedDate.minus(DatePeriod(months = 1))
                    runCatching { LocalDate(previousMonthDate.year, previousMonthDate.monthNumber, 1) }
                        .getOrNull()?.let { onLoadDate(it) }
                },
                onNextClick = {
                    val nextMonthDate = uiState.selectedDate.plus(DatePeriod(months = 1))
                    runCatching { LocalDate(nextMonthDate.year, nextMonthDate.monthNumber, 1) }
                        .getOrNull()?.let { onLoadDate(it) }
                }
            )
        },
        containerColor = MainBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MainBackground)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                NutritionCard(
                    unfilteredItems    = uiState.monthlyItems,
                    filteredItemCount  = filteredMonthlyItems.size,
                    categories         = categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onCategorySelect   = onSelectCategoryFilter,
                    boxScale           = uiState.boxScale,
                    calendarScale      = uiState.calendarScale,
                    onBoxScaleChange   = onBoxScaleChange,
                    onCalendarScaleChange = onCalendarScaleChange
                )

                Spacer(modifier = Modifier.height(16.dp))

                CalendarCard(
                    selectedYear  = uiState.selectedDate.year,
                    selectedMonth = uiState.selectedDate.monthNumber,
                    onDateClick   = { day ->
                        runCatching {
                            LocalDate(uiState.selectedDate.year, uiState.selectedDate.monthNumber, day)
                        }.getOrNull()?.let { selectedDate ->
                            onLoadDate(selectedDate)
                            showDetailSheet = true
                        }
                    },
                    monthlyItems = filteredMonthlyItems,
                    scale        = uiState.calendarScale
                )

                Spacer(modifier = Modifier.height(DiaryBottomContentPadding))
            }

            AddActionButton(
                onClick = {
                    selectedItemForEdit = null
                    showSourceScreen = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = DiaryAddButtonBottomPadding)
            )
        }
    }

    // ── POPUPS / OVERLAYS ──────────────────────────────────────────────────

    if (showDetailSheet) {
        DayDetailBottomSheet(
            onDismiss    = { showDetailSheet = false },
            selectedDate = uiState.selectedDate,
            items        = filteredItems,
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
        ModalBottomSheet(
            onDismissRequest = { showSourceScreen = false },
            sheetState = sourceSheetState,
            containerColor = MainBackground
        ) {
            AddFoodSourceScreen(
                categories  = categories,
                onBack      = { showSourceScreen = false },
                onImageCaptured = { uri ->
                    onImageSelected(uri)
                    showSourceScreen    = false
                    preSelectedCategory = null
                    showEntryScreen     = true
                },
                onPresetSelected = { category ->
                    preSelectedCategory = category
                    showSourceScreen    = false
                    showEntryScreen     = true
                },
                onManualSelected = {
                    showSourceScreen = false
                    showEntryScreen  = true
                }
            )
        }
    }

    if (showEntryScreen) {
        ModalBottomSheet(
            onDismissRequest = {
                showEntryScreen     = false
                selectedItemForEdit = null
                preSelectedCategory = null
                onClearPendingImage()
            },
            sheetState = entrySheetState,
            containerColor = MainBackground
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MainBackground) {
                val editingItem = selectedItemForEdit
                FoodEntryScreen(
                    existingItem        = editingItem,
                    preSelectedCategory = preSelectedCategory,
                    categories          = categories,
                    manageCategories    = manageCategories,
                    pendingImageUri     = pendingImageUri,
                    categoryDeleteError = categoryDeleteError,
                    onDismiss = {
                        showEntryScreen     = false
                        selectedItemForEdit = null
                        preSelectedCategory = null
                        onClearPendingImage()
                    },
                    onSave = { name, price, categoryId, rating, note, timeType, isShared, pickedTimeMillis ->
                        if (editingItem == null) {
                            onSaveItem(name, price, categoryId, rating, note, timeType, isShared, pickedTimeMillis)
                        } else {
                            onUpdateItem(editingItem.itemId, name, price, categoryId, rating, note, timeType, isShared, pickedTimeMillis)
                        }
                        showEntryScreen     = false
                        selectedItemForEdit = null
                        preSelectedCategory = null
                    },
                    onDelete = { itemId ->
                        onDeleteItem(itemId)
                        showEntryScreen     = false
                        selectedItemForEdit = null
                    },
                    onToggleCategoryVisibility = onToggleCategoryVisibility,
                    onDeleteCategory           = onDeleteCategory,
                    onEditCategory             = onEditCategory,
                    onCreateCategory           = onCreateCategory,
                    onClearCategoryError       = onClearCategoryError
                )
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
                streak        = 5,
                datesWithData = setOf(4, 5, 6),
                totalSpend    = 45000.0,
                itemCount     = 2
            ),
            pendingImageUri        = null,
            categories             = listOf(
                DiaryCategory("1", "Cơm",      "🍚", isSystem = true),
                DiaryCategory("2", "Mì & Phở", "🍜", isSystem = true)
            ),
            manageCategories       = listOf(
                DiaryCategory("1", "Rice", "🍚", isSystem = true),
                DiaryCategory("2", "Noodles", "🍜", isSystem = true),
                DiaryCategory("3", "Hidden", "👀", isHidden = true)
            ),
            categoryDeleteError        = null,
            triggerAdd                 = false,
            itemSaved                  = false,
            onConsumeItemSaved         = {},
            onClearPendingImage        = {},
            onClearCategoryError       = {},
            onAddTriggered             = {},
            onLoadDate                 = {},
            onImageSelected            = {},
            onSaveItem                 = { _, _, _, _, _, _, _, _ -> },
            onUpdateItem               = { _, _, _, _, _, _, _, _, _ -> },
            onDeleteItem               = {},
            onDeleteCategory           = {},
            onToggleCategoryVisibility = {},
            onEditCategory             = { _, _, _ -> },
            onCreateCategory           = { _, _ -> },
            onSelectCategoryFilter     = {},
            onBoxScaleChange           = {},
            onCalendarScaleChange      = {}
        )
    }
}