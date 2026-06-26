package com.SE114.food_tracker.feature.diary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.core.util.*
import java.util.Calendar

@Composable
fun DayItem(
    name: String,
    category: String,
    price: Double,
    createdAt: Long,
    timeLabel: String,
    categoryIcon: String = "",
    imageUrl: String? = null,
    onClick: () -> Unit
) {
    val displayTime = remember(createdAt) {
        val calendar = Calendar.getInstance().apply { timeInMillis = createdAt }
        String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(LightPeach, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Ảnh món ăn",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = categoryIcon.ifEmpty { "🍱" },
                        fontSize = 22.sp
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = category, style = AppTypography.labelMedium)
                    Spacer(Modifier.width(12.dp))

                    // Badge hiển thị: "15:45 • Chiều"
                    Surface(
                        color = Color(0xFFE2E2E2),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$displayTime • $timeLabel",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Text(
                text = LocalCurrencyDisplay.current.format(price),
                color = OrangeMain,
                style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DayItemPreview() {
    FoodTrackerTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DayItem(
                name = "Phở Hà Nội",
                category = "Mì & Phở",
                price = 30000.0,
                createdAt = System.currentTimeMillis(),
                timeLabel = "Chiều",
                categoryIcon = "🍜",
                imageUrl = null,
                onClick = {}
            )
        }
    }
}