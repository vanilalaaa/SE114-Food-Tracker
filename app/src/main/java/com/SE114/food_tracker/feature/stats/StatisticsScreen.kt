package com.SE114.food_tracker.feature.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.components.*
import com.SE114.food_tracker.feature.stats.components.*
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun StatisticsScreen() {
    val scrollState = rememberScrollState()

    // 4 tab thời gian
    var selectedTab by remember { mutableStateOf(0) }

    // 2 tab nội dung
    var selectedContentTab by remember { mutableStateOf(1) }

    // Quản lý ngân sách
    var dailyBudget by remember { mutableStateOf(100000f) }
    var weeklyBudget by remember { mutableStateOf(500000f) }
    var monthlyBudget by remember { mutableStateOf(2000000f) }
    var yearlyBudget by remember { mutableStateOf(24000000f) }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf("") }

    val totalSpendingAmount = 120000f

    val currentBudget = when (selectedTab) {
        1 -> weeklyBudget
        2 -> monthlyBudget
        3 -> yearlyBudget
        else -> dailyBudget
    }

    val remainingBudget = currentBudget - totalSpendingAmount
    val isBudgetExceeded = remainingBudget < 0
    val isWeeklyMode = selectedTab != 0

    Scaffold(
        topBar = {
            StatisticsTopBar(
                dateText = when (selectedTab) {
                    1 -> "Tuần 23 (01/06 - 07/06)"
                    2 -> "Tháng 06/2026"
                    3 -> "Năm 2026"
                    else -> "03/04/2026"
                },
                onDateClick = {
                    budgetInput = currentBudget.toInt().toString()
                    showBudgetDialog = true
                },
                onNextClick = {},
                onPreviousClick = {}
            )
        },
        bottomBar = {
            BottomBar(onItemSelected = {})
        },
        containerColor = MainBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            TimeRangeSelector(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

            TabRow(
                selectedTabIndex = selectedContentTab,
                containerColor = MainBackground,
                contentColor = StatPinkDark
            ) {
                Tab(
                    selected = selectedContentTab == 0,
                    onClick = { selectedContentTab = 0 },
                    text = { Text("Chi tiêu", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedContentTab == 1,
                    onClick = { selectedContentTab = 1 },
                    text = { Text("Phân tích", fontWeight = FontWeight.Bold) }
                )
            }

            if (isBudgetExceeded) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF2F2)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = "Chi tiêu hiện tại đã vượt quá ngân sách kỳ này!",
                            color = Color(0xFF9B1C1C),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (selectedContentTab == 0) {
                // ================== CHI TIÊU ==================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            budgetInput = currentBudget.toInt().toString()
                            showBudgetDialog = true
                        }
                ) {
                    StatisticsSummaryGrid(
                        totalMeals = "${currentBudget.toInt() / 1000}K",
                        totalSpending = "${totalSpendingAmount.toInt() / 1000}K",
                        averagePerMeal = if (isBudgetExceeded) "-${-remainingBudget.toInt() / 1000}K" else "${remainingBudget.toInt() / 1000}K"
                    )
                }

                ChartCard(
                    title = "Chi tiêu theo buổi",
                    data = listOf(
                        Pair("Sáng", 20),
                        Pair("Trưa", 40),
                        Pair("Chiều", 60),
                        Pair("Tối", 0)
                    )
                )

                //================== BIỂU ĐỒ TRÒN ==================
                LocalDonutChartCard(
                    title = "Tỷ trọng chi tiêu theo danh mục",
                    categories = listOf(
                        DonutSegment("Mì & Phở", 70000f, Color(0xFFE8AEB4)),
                        DonutSegment("Cơm", 30000f, Color(0xFFFBE3B5)),
                        DonutSegment("Nước uống", 20000f, Color(0xFFAED9E0))
                    )
                )

                PopularFoodCard(
                    foodList = listOf(PopularFoodData(name = "Phở Hà Nội", recordCount = "1"))
                )

                DetailCardSection(
                    dataGroups = listOf(
                        DayGroup(
                            dateLabel = "Thứ Sáu, 03/04",
                            meals = listOf(
                                MealRecord("15:20", "Phở Hà Nội", "Mì & Phở", "70k đ"),
                                MealRecord("18:45", "Cơm tấm", "Cơm", "30k đ")
                            )
                        )
                    ),
                    isWeeklyMode = isWeeklyMode
                )
            } else {
                // ================== PHÂN TÍCH ==================
                LocalLineTrendChartCard(
                    title = "Dự báo xu hướng kỳ tới",
                    points = listOf(45f, 70f, 110f, 130f)
                )

                Text(
                    text = "CHỈ SỐ CHUYÊN SÂU",
                    style = StatSectionTitleStyle,
                    color = TextPrimaryStat,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                InsightDashboardGrid(
                    topFood = "Phở Hà Nội",
                    topCategory = "Mì & Phở",
                    variationText = "+15.4% (Tăng)"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // --- DIALOG ĐẶT NGÂN SÁCH ---
    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = {
                Text(
                    "Đặt ngân sách " + when (selectedTab) {
                        1 -> "Tuần"
                        2 -> "Tháng"
                        3 -> "Năm"
                        else -> "Ngày"
                    }, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hạn mức mong muốn (đ):")
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Số tiền") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val value = budgetInput.toFloatOrNull() ?: 0f
                        when (selectedTab) {
                            1 -> weeklyBudget = value
                            2 -> monthlyBudget = value
                            3 -> yearlyBudget = value
                            else -> dailyBudget = value
                        }
                        showBudgetDialog = false
                    }
                ) { Text("Lưu lại") }
            },
            dismissButton = {
                Button(onClick = { showBudgetDialog = false }) { Text("Hủy") }
            }
        )
    }
}

@Preview(showSystemUi = true)
@Composable
fun StatisticsScreenPreview() {
    FoodTrackerTheme {
        StatisticsScreen()
    }
}