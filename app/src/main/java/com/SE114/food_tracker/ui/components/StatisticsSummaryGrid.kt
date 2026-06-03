package com.SE114.food_tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun SummaryItem(
    label: String,
    value: String,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isHighlighted) StatPinkDark else CardWhite
    val labelColor = if (isHighlighted) Color.White else TextLabelGray
    val valueColor = if (isHighlighted) Color.White else TextPrimaryStat

    Surface(
        modifier = modifier.height(85.dp),
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = StatLabelStyle,
                color = labelColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = StatValueStyle,
                color = valueColor
            )
        }
    }
}

@Composable
fun StatisticsSummaryGrid(
    totalMeals: String,
    totalSpending: String,
    averagePerMeal: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryItem(
            label = "TỔNG MÓN",
            value = totalMeals,
            isHighlighted = false,
            modifier = Modifier.weight(1f)
        )
        SummaryItem(
            label = "TỔNG CHI",
            value = totalSpending,
            isHighlighted = true,
            modifier = Modifier.weight(1.1f)
        )
        SummaryItem(
            label = "TB / MÓN",
            value = averagePerMeal,
            isHighlighted = false,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StatisticsSummaryGridPreview() {
    FoodTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MainBackground)
                .padding(16.dp)
        ) {
            StatisticsSummaryGrid(
                totalMeals = "1",
                totalSpending = "30K Đ",
                averagePerMeal = "30K Đ"
            )
        }
    }
}