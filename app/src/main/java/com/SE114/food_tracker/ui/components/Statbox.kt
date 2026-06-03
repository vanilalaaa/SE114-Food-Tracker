package com.SE114.food_tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = CalendarHighlight, // Màu #FCDFCF
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = AppTypography.titleMedium.copy(color = StatRed))
            Text(label, style = AppTypography.labelMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatBoxPreview() {
    FoodTrackerTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatBox(label = "Món", value = "1", modifier = Modifier.weight(1f))
            StatBox(label = "Chi", value = "30k đ", modifier = Modifier.weight(1f))
        }
    }
}