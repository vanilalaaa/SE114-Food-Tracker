package com.SE114.food_tracker.feature.diary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun TimeSelector(
    time: String,
    session: String,
    icon: String,
    onTimeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessionTextColor = when (session) {
        "Sáng"  -> SettingActionOrange
        "Trưa"  -> OrangeMain
        "Chiều" -> AlertGreen
        else    -> DarkPink
    }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = "THỜI GIAN",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onTimeClick() },
            color = CardWhite,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = TextLabelGray,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = time,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )

                Spacer(Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 18.dp)
                        .width(1.dp)
                        .background(HintGray.copy(alpha = 0.3f))
                )

                Spacer(Modifier.width(16.dp))

                Text(
                    text = icon,
                    fontSize = 20.sp
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "Buổi $session",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = sessionTextColor,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = HintGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimeSelectorPreview() {
    FoodTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MainBackground)
                .padding(16.dp)
        ) {
            TimeSelector(
                time = "12:30",
                session = "Trưa",
                icon = "☀️",
                onTimeClick = {}
            )
        }
    }
}