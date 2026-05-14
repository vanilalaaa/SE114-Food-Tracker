package com.SE114.food_tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.ui.theme.*

@Composable
fun NutritionCard(
    onMenuClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 24.dp),
        color = LightPinkBG,
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Thêm tùy chọn",
                    tint = TextPrimary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NutritionCardPreview() {
    FoodTrackerTheme {
        Box(modifier = Modifier.padding(vertical = 20.dp)) {
            NutritionCard(onMenuClick = {})
        }
    }
}