package com.SE114.food_tracker.feature.diary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

@Composable
fun CalendarCard(
    selectedYear: Int,
    selectedMonth: Int,
    onDateClick: (Int) -> Unit,
    hasDataDates: List<Int> = emptyList()
) {
    val days = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    val calendarGridItems = remember(selectedYear, selectedMonth) {
        val firstDayOfMonth = LocalDate(selectedYear, selectedMonth, 1)

        val emptySlotsBefore = firstDayOfMonth.dayOfWeek.isoDayNumber - 1

        val nextMonth = if (selectedMonth == 12) 1 else selectedMonth + 1
        val nextMonthYear = if (selectedMonth == 12) selectedYear + 1 else selectedYear
        val firstDayOfNextMonth = LocalDate(nextMonthYear, nextMonth, 1)
        val lastDayOfMonth = firstDayOfNextMonth.minus(kotlinx.datetime.DatePeriod(days = 1)).dayOfMonth

        List(emptySlotsBefore) { null } + (1..lastDayOfMonth).toList()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .height(380.dp),
        shape = RoundedCornerShape(25.dp),
        color = CalendarHighlight,
        shadowElevation = 10.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                days.forEach {
                    Text(
                        text = it,
                        color = DarkPink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(calendarGridItems) { date ->
                    if (date != null) {
                        val isToday = (date == today.dayOfMonth &&
                                selectedMonth == today.monthNumber &&
                                selectedYear == today.year)

                        val hasData = hasDataDates.contains(date)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onDateClick(date) }
                                .padding(vertical = 4.dp)
                        ) {
                            // Ô hiển thị Số ngày
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(32.dp)
                            ) {
                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(LightGreen, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            date.toString(),
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        date.toString(),
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        if (hasData) Color.White else Color.Transparent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasData) {
                                    Icon(
                                        Icons.Default.Restaurant,
                                        null,
                                        modifier = Modifier.size(12.dp),
                                        tint = OrangeMain
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarCardPreview() {
    FoodTrackerTheme {
        CalendarCard(
            selectedYear = 2026,
            selectedMonth = 6,
            onDateClick = {}
        )
    }
}