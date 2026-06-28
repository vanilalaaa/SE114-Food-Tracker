package com.SE114.food_tracker.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

@Composable
fun DayPickerDialog(
    selectedDay: Int,
    selectedMonth: Int,
    selectedYear: Int,
    onDateClick: (Int) -> Unit,
    onMonthYearChanged: (month: Int, year: Int) -> Unit,
    hasDataDates: List<Int> = emptyList()
) {
    val days = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

    // Giữ lại để nhận diện ngày hôm nay thực tế trên hệ thống (Real Today)
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    var showMonthPicker by remember { mutableStateOf(false) }

    val calendarGridItems = remember(selectedYear, selectedMonth) {
        val firstDayOfMonth = LocalDate(selectedYear, selectedMonth, 1)
        val emptySlotsBefore = firstDayOfMonth.dayOfWeek.isoDayNumber - 1

        val nextMonth = if (selectedMonth == 12) 1 else selectedMonth + 1
        val nextMonthYear = if (selectedMonth == 12) selectedYear + 1 else selectedYear
        val firstDayOfNextMonth = LocalDate(nextMonthYear, nextMonth, 1)
        val lastDayOfMonth = firstDayOfNextMonth.minus(DatePeriod(days = 1)).dayOfMonth

        List(emptySlotsBefore) { null } + (1..lastDayOfMonth).toList()
    }

    fun prevMonth(): Pair<Int, Int> =
        if (selectedMonth == 1) 12 to selectedYear - 1
        else selectedMonth - 1 to selectedYear

    fun nextMonth(): Pair<Int, Int> =
        if (selectedMonth == 12) 1 to selectedYear + 1
        else selectedMonth + 1 to selectedYear

    if (showMonthPicker) {
        MonthYearPickerDialog(
            currentMonth = selectedMonth,
            currentYear  = selectedYear,
            onDismiss    = { showMonthPicker = false },
            onConfirm    = { month, year ->
                showMonthPicker = false
                onMonthYearChanged(month, year)
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Month-year navigation header ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val (m, y) = prevMonth()
                    onMonthYearChanged(m, y)
                }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Tháng trước",
                        tint = Color.Black
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showMonthPicker = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "tháng $selectedMonth $selectedYear",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Chọn tháng/năm",
                        tint = Color.Black
                    )
                }

                IconButton(onClick = {
                    val (m, y) = nextMonth()
                    onMonthYearChanged(m, y)
                }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Tháng sau",
                        tint = Color.Black
                    )
                }
            }

            // ── Day-of-week header row ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                days.forEach {
                    Text(
                        text = it,
                        color = DarkPink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Calendar Grid ────────────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(calendarGridItems) { date ->
                    if (date != null) {
                        // Trạng thái ngày đang được chọn (Theo anchor của ViewModel)
                        val isSelected = (date == selectedDay)

                        // Nhận biết xem ô này có phải là ngày hôm nay thực tế không
                        val isRealToday = (date == today.dayOfMonth
                                && selectedMonth == today.monthNumber
                                && selectedYear == today.year)

                        val hasData = hasDataDates.contains(date)

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (hasData) CalendarHighlight else Color.Transparent)
                                .clickable { onDateClick(date) }
                        ) {
                            if (isSelected) {
                                // Ô tròn MintGreen di chuyển linh hoạt theo ngày được chọn
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MintGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = date.toString(),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Text(
                                    text = date.toString(),
                                    // Nếu là ngày hôm nay thực tế nhưng chưa kích chọn thì text có màu MintGreen để làm dấu
                                    color = if (isRealToday) MintGreen else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isRealToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DayPickerDialogPreview() {
    FoodTrackerTheme {
        DayPickerDialog(
            selectedDay        = 28,
            selectedMonth      = 6,
            selectedYear       = 2026,
            onDateClick        = {},
            onMonthYearChanged = { _, _ -> }
        )
    }
}