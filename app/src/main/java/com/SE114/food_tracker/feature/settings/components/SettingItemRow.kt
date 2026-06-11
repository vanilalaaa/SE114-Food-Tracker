package com.SE114.food_tracker.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun SettingItemRow(
    title: String,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    valueBadge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isDestructive) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete",
                    tint = StatRed,
                    modifier = Modifier.padding(end = 12.dp).size(20.dp)
                )
            }
            Text(
                text = title,
                style = AppTypography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive) StatRed else TextSecondary
                )
            )
        }

        if (valueBadge != null) {
            Box(
                modifier = Modifier
                    .background(color = MintGreen.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = valueBadge,
                    color = MintGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        } else if (!isDestructive) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Go",
                tint = HintGray
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF5E4)
@Composable
fun SettingItemRowPreview() {
    FoodTrackerTheme {
        Column {
            SettingItemRow(title = "Đơn vị tiền", valueBadge = "đ VND", onClick = {})
        }
    }
}