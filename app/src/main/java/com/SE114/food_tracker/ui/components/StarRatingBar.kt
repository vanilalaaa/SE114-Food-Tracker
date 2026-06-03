package com.SE114.food_tracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme

@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 1..5) {
            Text(
                text = if (i <= rating) "⭐" else "☆",
                fontSize = 28.sp,
                modifier = Modifier.clickable { onRatingChange(i) }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF5E4)
@Composable
fun StarRatingBarPreview() {
    FoodTrackerTheme {
        StarRatingBar(rating = 4, onRatingChange = {})
    }
}