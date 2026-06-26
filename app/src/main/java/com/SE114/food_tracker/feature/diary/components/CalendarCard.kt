package com.SE114.food_tracker.feature.diary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .height(410.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 8.dp
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(calendarGridItems) { date ->
                    if (date != null) {
                        val isToday = (date == today.dayOfMonth && selectedMonth == today.monthNumber && selectedYear == today.year)
                        val dayPreviews = previewsByDay[date].orEmpty()
                        val hasData = dayPreviews.isNotEmpty() || hasDataDates.contains(date)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (hasData) CalendarHighlight else Color.Transparent)
                                .clickable { onDateClick(date) }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(36.dp)
                            ) {
                                if (isToday) {
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
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            FoodPreviewStack(
                                previews = dayPreviews,
                                hasData = hasData,
                                scale = scale
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodPreviewStack(
    previews: List<CalendarFoodPreview>,
    hasData: Boolean,
    scale: Float
) {
    if (!hasData) {
        Spacer(modifier = Modifier.height(42.dp))
        return
    }

    val avatarSize = (30f * scale.coerceIn(0.9f, 1.15f)).dp
    val step = (avatarSize.value * 0.44f).dp
    val visibleCount = previews.take(4).size

    val stackHeight = if (visibleCount > 2) (avatarSize + step) else avatarSize

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(stackHeight),
        contentAlignment = Alignment.TopCenter
    ) {
        val positions = when (visibleCount) {
            0, 1 -> listOf(0.dp to 0.dp)
            2 -> listOf(
                -(step / 2) to 0.dp,
                (step / 2) to 0.dp
            )
            3 -> listOf(
                -(step / 2) to 0.dp,
                (step / 2) to 0.dp,
                0.dp to step
            )
            else -> listOf(
                -(step / 2) to 0.dp,
                (step / 2) to 0.dp,
                -(step / 2) to step,
                (step / 2) to step
            )
        }

        previews.take(4).forEachIndexed { index, preview ->
            val (xOffset, yOffset) = positions[index]

            CalendarFoodAvatar(
                preview = preview,
                size = avatarSize,
                modifier = Modifier
                    .offset(x = xOffset, y = yOffset)
                    .zIndex(index.toFloat())
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
            .background(LightPeach)
            .border(2.dp, Color.White, CircleShape),
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
