package com.SE114.food_tracker.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun OutdatedRateWarningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightPeach)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Warning, contentDescription = "Cảnh báo", tint = SettingActionOrange, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Tỷ giá có thể đã cũ do không có kết nối mạng.",
            style = AppTypography.labelMedium,
            color = SettingActionOrange
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OutdatedRateWarningBannerPreview() {
    FoodTrackerTheme {
        OutdatedRateWarningBanner()
    }
}