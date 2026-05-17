package com.SE114.food_tracker.ui.screens.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.ui.components.*
import com.SE114.food_tracker.ui.theme.*

@Composable
fun StatisticsScreen() {
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableStateOf(0)}
    Scaffold(
        topBar = {
            StatisticsTopBar(
                dateText = "03/04/2026",
                onDateClick = {},
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
            TimeRangeSelector(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
            InsightCard(insightText = "Thử 1 món mới")
            Text(
                text = "Món theo buổi",
                style = StatSectionTitleStyle,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )
            ChartCard(
                title = "Món theo buổi",
                data = listOf(
                    Pair("Sáng", 0),
                    Pair("Trưa", 0),
                    Pair("Chiều", 1),
                    Pair("Tối", 0),
                    Pair("Khuya", 0)
                )
            )
            StatisticsSummaryGrid(
                totalMeals = "1",
                totalSpending = "30K",
                averagePerMeal = "30K"
            )
            Text(
                text = "PHỔ BIẾN NHẤT",
                style = StatSectionTitleStyle.copy(color = TextLabelGray), // Dùng màu Gray label như Figma
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )
            PopularFoodCard(
                foodList = listOf(
                PopularFoodData(name = "Phở Hà Nội", recordCount = "1")
                )
            )
            Text(
                text = "NHẬN XÉT",
                style = StatSectionTitleStyle.copy(color = TextLabelGray),
                modifier = Modifier.padding(start = 8.dp)
            )
            DetailCardSection(
                dataGroups = listOf(
                    DayGroup(
                        meals = listOf(
                            MealRecord(name = "Thử 1 món mới")
                        )
                    )
                ),
                isWeeklyMode = false
            )
            Text(
                text = "CHI TIẾT",
                style = StatSectionTitleStyle.copy(color = TextLabelGray),
                modifier = Modifier.padding(start = 8.dp)
            )
            DayItem(
                time = "15:20",
                name = "Phở Hà Nội",
                category = "Mì & Phở",
                price = 30000.0,
                onClick = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
