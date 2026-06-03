package com.SE114.food_tracker.feature.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.data.local.entities.Category
import com.SE114.food_tracker.ui.components.*
import com.SE114.food_tracker.data.local.entities.Item

// --- MOCK DATA ---
object DiaryMockData {
    val categories = listOf(
        Category(categoryId = "1", name = "Cơm", iconUrl = ""),
        Category(categoryId = "2", name = "Mì & Phở", iconUrl = "")
    )
    val items = listOf(
        Item(itemId = "1", categoryId = "2", name = "Phở Hà Nội", timeType = 1, price = 30000.0, entryDate = 1713830400000L)
    )
    const val STREAK = "1"
    const val DISPLAY_MONTH = "thg 4 2026"
}

@Composable
fun DiaryScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    var showDetailSheet by remember { mutableStateOf(false) }
    var showEntryScreen by remember { mutableStateOf(false) }
    var showSourceScreen by remember { mutableStateOf(false) }
    var selectedItemForEdit by remember { mutableStateOf<Item?>(null) }

    val scrollState = rememberScrollState()

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
                            streakCount = DiaryMockData.STREAK,
                            currentMonth = DiaryMockData.DISPLAY_MONTH,
                            onMonthClick = { showDetailSheet = true }
                        )
                        NutritionCard(onMenuClick = { /* Logic menu */ })
                        Spacer(modifier = Modifier.height(16.dp))
                        CalendarCard(onDateClick = { showDetailSheet = true })
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
                1 -> {
                    Text("Màn hình Thống kê", modifier = Modifier.padding(24.dp))
                }
                2 -> { // TAB CHI TIÊU
                    Text("Màn hình Chi tiêu", modifier = Modifier.padding(24.dp))
                }
                3 -> { // TAB CÀI ĐẶT
                    Text("Màn hình Cài đặt", modifier = Modifier.padding(24.dp))
                }
            }
        }

        if (showDetailSheet) {
            DayDetailBottomSheet(
                onDismiss = { showDetailSheet = false },
                items = DiaryMockData.items,
                categories = DiaryMockData.categories,
                onEditItem = { item ->
                    selectedItemForEdit = item
                    showDetailSheet = false
                    showEntryScreen = true
                }
            )
        }

        if (showSourceScreen) {
            AddFoodSourceScreen(
                onBack = { showSourceScreen = false },
                onSourceSelected = { showSourceScreen = false; showEntryScreen = true }
            )
        }

        if (showEntryScreen) {
            FoodEntryScreen(
                onDismiss = { showEntryScreen = false },
                onSave = { name, price, catId, rate, note ->
                    //Viết logic lưu vào Database ở đây
                    showEntryScreen = false
                }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DiaryScreenPreview() {
    FoodTrackerTheme {
        DiaryScreen()
    }
}