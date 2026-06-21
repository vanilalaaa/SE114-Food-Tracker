package com.SE114.food_tracker.feature.diary.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.MintGreen

@Composable
fun ShareToggle(
    isShared: Boolean,
    onShareChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = "CHIA SẺ",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShareChange(!isShared) },
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isShared) Icons.Default.Group else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isShared) MintGreen else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.padding(end = 8.dp)) {
                        Text(
                            text = "Chia sẻ với bạn bè",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = if (isShared) {
                                "Bạn bè sẽ thấy món này trong nhật ký của bạn"
                            } else {
                                "Chỉ riêng bạn nhìn thấy món này"
                            },
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 16.sp
                        )
                    }
                }

                Switch(
                    checked = isShared,
                    onCheckedChange = onShareChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MintGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.LightGray
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF5E4)
@Composable
private fun ShareTogglePreviewOff() {
    FoodTrackerTheme {
        ShareToggle(
            isShared = false,
            onShareChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF5E4)
@Composable
private fun ShareTogglePreviewOn() {
    FoodTrackerTheme {
        ShareToggle(
            isShared = true,
            onShareChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
