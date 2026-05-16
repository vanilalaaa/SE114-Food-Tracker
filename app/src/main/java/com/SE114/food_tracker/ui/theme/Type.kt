package com.SE114.food_tracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TextPrimary),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary),
    titleSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, color = TextSecondary),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, color = HintGray)
)