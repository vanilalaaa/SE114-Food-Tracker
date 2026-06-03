package com.SE114.food_tracker.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val FoodTrackerColorScheme = lightColorScheme(
    primary = MintGreen,
    secondary = OrangeMain,
    background = MainBackground,
    surface = CardWhite,
    onBackground = TextPrimary,
    onSurface = TextSecondary
)

@Composable
fun FoodTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FoodTrackerColorScheme,
        typography = AppTypography,
        content = content
    )
}