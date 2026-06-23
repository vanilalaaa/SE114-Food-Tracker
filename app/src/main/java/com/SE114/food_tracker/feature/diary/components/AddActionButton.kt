package com.SE114.food_tracker.feature.diary.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.MintGreen

@Composable
fun AddActionButton(
    onClick: () -> Unit,
    contentDescription: String = "Thêm món ăn"
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MintGreen,
        contentColor = Color.White,
        shape = CircleShape
    ) {
        Icon(Icons.Default.Add, contentDescription = contentDescription)
    }
}

@Preview(showBackground = true)
@Composable
fun AddActionButtonPreview() {
    FoodTrackerTheme {
        AddActionButton(onClick = {})
    }
}
