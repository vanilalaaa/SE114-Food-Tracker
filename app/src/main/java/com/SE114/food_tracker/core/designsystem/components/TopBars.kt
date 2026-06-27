package com.SE114.food_tracker.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.AppTypography
import com.SE114.food_tracker.core.designsystem.theme.CardWhite
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.MainBackground
import com.SE114.food_tracker.core.designsystem.theme.OrangeMain
import com.SE114.food_tracker.core.designsystem.theme.StatTabActiveStyle
import com.SE114.food_tracker.core.designsystem.theme.TextLabelGray
import com.SE114.food_tracker.core.designsystem.theme.TextSecondary

@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    titleSuffix: (@Composable RowScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MainBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navigationIcon?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f, fill = false),
                        style = AppTypography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    titleSuffix?.invoke(this)
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = TextLabelGray,
                        style = AppTypography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = actions
        )
    }
}

@Composable
fun DiaryTopBar(
    streakCount: String,
    currentMonth: String,
    onMonthClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    AppTopBar(
        title = "Nhật ký",
        titleSuffix = {
            Row(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .widthIn(min = 42.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔥",
                    fontSize = 24.sp,
                    lineHeight = 24.sp,
                    maxLines = 1
                )
                Text(
                    text = streakCount,
                    color = OrangeMain,
                    style = AppTypography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        },
        actions = {
            DateNavigator(
                dateText = currentMonth,
                previousContentDescription = "Tháng trước",
                nextContentDescription = "Tháng sau",
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onDateClick = onMonthClick
            )
        }
    )
}

@Composable
fun StatisticsTopBar(
    dateText: String,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppTopBar(
        title = "Thống kê",
        modifier = modifier,
        actions = {
            DateNavigator(
                dateText = dateText,
                previousContentDescription = "Previous",
                nextContentDescription = "Next",
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onDateClick = onDateClick
            )
        }
    )
}

@Composable
private fun DateNavigator(
    dateText: String,
    previousContentDescription: String,
    nextContentDescription: String,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onDateClick: () -> Unit
) {
    Surface(
        modifier = Modifier.widthIn(max = 168.dp),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = previousContentDescription,
                tint = TextSecondary,
                modifier = Modifier
                    .size(22.dp)
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
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Calendar",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = dateText,
                    modifier = Modifier.widthIn(max = 78.dp),
                    style = StatTabActiveStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = nextContentDescription,
                tint = TextSecondary,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onNextClick() }
            )
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
                onPreviousClick = {},
                onMonthClick = {},
                onNextClick = {}
            )
            StatisticsTopBar(
                dateText = "03/04/2026",
                onPreviousClick = {},
                onNextClick = {},
                onDateClick = {}
            )
            AppTopBar(title = "Newsfeed", subtitle = "12 bài viết")
        }
    }
}
