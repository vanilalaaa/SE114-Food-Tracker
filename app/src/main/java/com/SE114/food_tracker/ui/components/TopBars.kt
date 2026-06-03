package com.SE114.food_tracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.core.designsystem.theme.*
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun DiaryTopBar(
    streakCount: String,
    currentMonth: String,
    onMonthClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp, 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Nhật ký", style = AppTypography.titleLarge)
            Text(text = " 🔥$streakCount", color = OrangeMain, style = AppTypography.titleLarge)
        }

        Surface(
            onClick = onMonthClick,
            shape = RoundedCornerShape(12.dp),
            color = CardWhite,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Calendar",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = currentMonth,
                    style = AppTypography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun StatisticsTopBar(
    dateText: String,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Thống kê",
            style = StatTitleStyle
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardWhite,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Previous",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onPreviousClick() }
                )

                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onDateClick() }
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Calendar",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = dateText,
                        style = StatTabActiveStyle
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Next",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNextClick() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TopBarsPreview() {
    FoodTrackerTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DiaryTopBar(
                streakCount = "1",
                currentMonth = "thg 4 2026",
                onMonthClick = {}
            )
            StatisticsTopBar(
                dateText = "03/04/2026",
                onPreviousClick = {},
                onNextClick = {},
                onDateClick = {}
            )
        }
    }
}