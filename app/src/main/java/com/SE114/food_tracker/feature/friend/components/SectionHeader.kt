package com.SE114.food_tracker.feature.friend.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun SectionHeader(title: String, count: Int, maxCount: Int? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(text = title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        if (maxCount != null) {
            Text(text = "$count/$maxCount", color = HintGray, fontSize = 14.sp)
        }
    }
}