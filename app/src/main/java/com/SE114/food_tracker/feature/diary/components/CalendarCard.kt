package com.SE114.food_tracker.feature.diary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.feature.diary.DiaryItem
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

private val CalendarTodayNumberSize = 32.dp
private val CalendarDataCellWidth = 40.dp
private val CalendarDataCellHeight = 65.dp
private val CalendarCardPadding = 16.dp
private val CalendarHeaderHeight = 18.dp
private val CalendarHeaderGridSpacing = 5.dp
private val CalendarRowSpacing = 2.dp

@Composable
fun CalendarCard(
    selectedYear: Int,
    selectedMonth: Int,
    onDateClick: (Int) -> Unit,
    hasDataDates: List<Int> = emptyList(),
    monthlyItems: List<DiaryItem> = emptyList(),
    scale: Float = 1f
) {
    val days = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val previewsByDay = remember(selectedYear, selectedMonth, monthlyItems) {
        monthlyItems
            .filter { item ->
                val date = Instant.fromEpochMilliseconds(item.entryDate)
                    .toLocalDateTime(TimeZone.UTC)
                    .date
                date.year == selectedYear && date.monthNumber == selectedMonth
            }
            .groupBy { item ->
                Instant.fromEpochMilliseconds(item.entryDate)
                    .toLocalDateTime(TimeZone.UTC)
                    .date
                    .dayOfMonth
            }
            .mapValues { (_, items) ->
                items
                    .sortedByDescending { it.createdAt }
                    .map { item ->
                        CalendarFoodPreview(
                            imageUrl = item.imageUrl,
                            fallback = item.categoryIconUrl
                                .ifBlank { item.categoryName.firstOrNull()?.uppercase() ?: "?" }
                        )
                    }
            }
    }

    val calendarGridItems = remember(selectedYear, selectedMonth) {
        val firstDayOfMonth = LocalDate(selectedYear, selectedMonth, 1)
        val emptySlotsBefore = firstDayOfMonth.dayOfWeek.isoDayNumber - 1

        val nextMonth = if (selectedMonth == 12) 1 else selectedMonth + 1
        val nextMonthYear = if (selectedMonth == 12) selectedYear + 1 else selectedYear
        val firstDayOfNextMonth = LocalDate(nextMonthYear, nextMonth, 1)
        val lastDayOfMonth = firstDayOfNextMonth.minus(kotlinx.datetime.DatePeriod(days = 1)).dayOfMonth

        List(emptySlotsBefore) { null } + (1..lastDayOfMonth).toList()
    }
    val calendarRows = remember(calendarGridItems) {
        val rowCount = (calendarGridItems.size + 6) / 7
        (0 until rowCount).map { rowIndex ->
            (0 until 7).map { columnIndex ->
                calendarGridItems.getOrNull(rowIndex * 7 + columnIndex)
            }
        }
    }
    val calendarHeight = CalendarCardPadding * 2 +
            CalendarHeaderHeight +
            CalendarHeaderGridSpacing +
            CalendarDataCellHeight * calendarRows.size +
            CalendarRowSpacing * (calendarRows.size - 1).coerceAtLeast(0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .height(calendarHeight),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(CalendarCardPadding)) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CalendarHeaderHeight),
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

            Spacer(Modifier.height(5.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CalendarRowSpacing)
            ) {
                calendarRows.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        week.forEach { date ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CalendarDateCell(
                                    date = date,
                                    today = today,
                                    selectedYear = selectedYear,
                                    selectedMonth = selectedMonth,
                                    previews = date?.let { previewsByDay[it].orEmpty() }.orEmpty(),
                                    hasData = date?.let { previewsByDay[it].orEmpty().isNotEmpty() || hasDataDates.contains(it) } == true,
                                    scale = scale,
                                    onDateClick = onDateClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDateCell(
    date: Int?,
    today: LocalDate,
    selectedYear: Int,
    selectedMonth: Int,
    previews: List<CalendarFoodPreview>,
    hasData: Boolean,
    scale: Float,
    onDateClick: (Int) -> Unit
) {
    if (date == null) {
        Spacer(
            modifier = Modifier.size(
                width = CalendarDataCellWidth,
                height = CalendarDataCellHeight
            )
        )
        return
    }

    val isToday = date == today.dayOfMonth &&
            selectedMonth == today.monthNumber &&
            selectedYear == today.year

    Box(
        modifier = Modifier
            .size(
                width = CalendarDataCellWidth,
                height = CalendarDataCellHeight
            )
            .clip(RoundedCornerShape(12.dp))
            .background(if (hasData) CalendarHighlight else Color.Transparent)
            .clickable { onDateClick(date) }
    ) {
        if (isToday) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(CalendarTodayNumberSize)
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
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 7.dp)
            )
        }

        FoodPreviewStack(
            previews = previews,
            hasData = hasData,
            scale = scale,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun FoodPreviewStack(
    previews: List<CalendarFoodPreview>,
    hasData: Boolean,
    scale: Float,
    modifier: Modifier = Modifier
) {
    if (!hasData || previews.isEmpty()) {
        return
    }

    val avatarSize = (28f * scale.coerceIn(0.5f, 2.2f)).dp
    val step = (avatarSize.value * 0.44f).dp
    val totalCount = previews.size
    val visiblePreviews = previews.take(2)
    val visibleCount = visiblePreviews.size

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        val positions = if (visibleCount == 2) {
            listOf(
                (step / 2) to 0.dp,
                -(step / 2) to 0.dp
            )
        } else {
            listOf(0.dp to 0.dp)
        }

        visiblePreviews.forEachIndexed { index, preview ->
            val (xOffset, yOffset) = positions[index]

            CalendarFoodAvatar(
                preview = preview,
                size = avatarSize,
                modifier = Modifier
                    .offset(x = xOffset, y = yOffset)
                    .zIndex((visibleCount - index).toFloat())
            )
        }

        if (totalCount >= 3) {
            Text(
                text = "x$totalCount",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex((visibleCount + 1).toFloat())
                    .background(Color.Black.copy(alpha = 0.36f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun CalendarFoodAvatar(
    preview: CalendarFoodPreview,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(LightPeach),
        contentAlignment = Alignment.Center
    ) {
        if (!preview.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = preview.imageUrl,
                contentDescription = "Ảnh món ăn",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = preview.fallback,
                color = OrangeMain,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class CalendarFoodPreview(
    val imageUrl: String?,
    val fallback: String
)

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
