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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SE114.food_tracker.core.designsystem.components.BottomBar
import com.SE114.food_tracker.core.designsystem.components.DiaryTopBar
import com.SE114.food_tracker.core.designsystem.components.NutritionCard
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.MainBackground
import com.SE114.food_tracker.feature.diary.components.AddActionButton
import com.SE114.food_tracker.feature.diary.components.CalendarCard
import com.SE114.food_tracker.feature.diary.components.CategoryRowItem
import kotlinx.datetime.LocalDate

@Composable
fun DiaryScreen() {
    val diaryViewModel    = hiltViewModel<DiaryViewModel>()
    val categoryViewModel = hiltViewModel<CategoryViewModel>()
    val uiState      by diaryViewModel.uiState.collectAsStateWithLifecycle()
    val categoryState by categoryViewModel.visibleCategories.collectAsStateWithLifecycle()

    val categories = categoryState.ifEmpty { uiState.categories }

    DiaryScreenContent(
        uiState = uiState,
        categories = categories,
        onLoadDate = { diaryViewModel.loadDate(it) },
        onSaveItem = { n, p, c, r, no, t -> diaryViewModel.saveItem(n, p, c, r, no, t) },
        onUpdateItem = { id, n, p, c, r, no, t -> diaryViewModel.updateItem(id, n, p, c, r, no, t) },
        onDeleteItem = { diaryViewModel.deleteItem(it) },
        onDeleteCategory = { categoryViewModel.deleteCategory(it) },
        onToggleCategoryVisibility = { categoryViewModel.toggleVisibility(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreenContent(
    uiState: DiaryUiState,
    categories: List<DiaryCategory>,
    onLoadDate: (LocalDate) -> Unit,
    onSaveItem: (String, Double, String, Int, String, Int) -> Unit,
    onUpdateItem: (String, String, Double, String, Int, String, Int) -> Unit,
    onDeleteItem: (String) -> Unit,
    onDeleteCategory: (DiaryCategory) -> Unit,
    onToggleCategoryVisibility: (DiaryCategory) -> Unit
) {
    var selectedTabIndex    by remember { mutableIntStateOf(0) }
    var showDetailSheet     by remember { mutableStateOf(false) }
    var showEntryScreen     by remember { mutableStateOf(false) }
    var showSourceScreen    by remember { mutableStateOf(false) }
    var selectedItemForEdit by remember { mutableStateOf<DiaryItem?>(null) }
    var showManageCategories by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val datePickerDialog = remember(uiState.selectedYear, uiState.selectedMonth, uiState.selectedDayOfMonth) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                runCatching { LocalDate(year, month + 1, dayOfMonth) }
                    .getOrNull()
                    ?.let { onLoadDate(it) }
            },
            uiState.selectedYear,
            uiState.selectedMonth - 1,
            uiState.selectedDayOfMonth
        )
    }

    Scaffold(
        bottomBar = {
            BottomBar(
                selectedIndex = selectedTabIndex,
                onItemSelected = { index -> selectedTabIndex = index }
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                AddActionButton(onClick = {
                    selectedItemForEdit = null
                    showSourceScreen = true
                })
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MainBackground)
        ) {
            when (selectedTabIndex) {
                0 -> {
                    Column(modifier = Modifier.verticalScroll(scrollState)) {
                        DiaryTopBar(
                            streakCount  = uiState.streak.toString(),
                            currentMonth = "Tháng ${uiState.selectedMonth} ${uiState.selectedYear}",
                            onMonthClick = { datePickerDialog.show() }
                        )
                        NutritionCard(onMenuClick = { })
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
                            hasDataDates = uiState.datesWithData.toList()
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
                1 -> Text("Màn hình Thống kê", modifier = Modifier.padding(24.dp))
                2 -> Text("Màn hình Chi tiêu",  modifier = Modifier.padding(24.dp))
                3 -> Text("Màn hình Cài đặt",   modifier = Modifier.padding(24.dp))
            }
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
            AddFoodSourceScreen(
                onBack = { showSourceScreen = false },
                onCameraSelected = {
                    showSourceScreen = false
                    showEntryScreen  = true
                },
                onGallerySelected = {
                    showSourceScreen = false
                    showEntryScreen  = true
                },
                onManualSelected = {
                    showSourceScreen = false
                    showEntryScreen  = true
                }
            )
        }

        if (showEntryScreen) {
            val editingItem = selectedItemForEdit
            FoodEntryScreen(
                existingItem = editingItem,
                categories   = categories,
                onDismiss    = {
                    showEntryScreen     = false
                    selectedItemForEdit = null
                },
                onSave = { name, price, categoryId, rating, note, timeType ->
                    if (editingItem == null) {
                        onSaveItem(name, price, categoryId, rating, note, timeType)
                    } else {
                        onUpdateItem(editingItem.itemId, name, price, categoryId, rating, note, timeType)
                    }
                    showEntryScreen     = false
                    selectedItemForEdit = null
                },
                onDelete = { itemId ->
                    onDeleteItem(itemId)
                    showEntryScreen     = false
                    selectedItemForEdit = null
                },
                onManageCategories = { showManageCategories = true }
            )
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
                datesWithData = setOf(4, 5, 6)
            ),
            categories = listOf(
                DiaryCategory("1", "Cơm", "🍚", isSystem = true),
                DiaryCategory("2", "Mì & Phở", "🍜", isSystem = true)
            ),
            onLoadDate = {},
            onSaveItem = { _, _, _, _, _, _ -> },
            onUpdateItem = { _, _, _, _, _, _, _ -> },
            onDeleteItem = {},
            onDeleteCategory = {},
            onToggleCategoryVisibility = {}
        )
    }
}