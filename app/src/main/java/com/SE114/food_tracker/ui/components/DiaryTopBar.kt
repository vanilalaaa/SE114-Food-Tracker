package com.SE114.food_tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun DiaryTopBar(streakCount: String, currentMonth: String, onMonthClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(24.dp, 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nhật ký", style = AppTypography.titleLarge)
            Text(" 🔥$streakCount", color = OrangeMain, style = AppTypography.titleLarge)
        }
        Surface(onClick = onMonthClick, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = CardWhite, shadowElevation = 2.dp) {
            Text(currentMonth, modifier = Modifier.padding(12.dp, 6.dp), style = AppTypography.bodyLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DiaryTopBarPreview() {
    FoodTrackerTheme {
        DiaryTopBar(
            streakCount = "1",
            currentMonth = "thg 4 2026",
            onMonthClick = {}
        )
    }
}