package com.SE114.food_tracker.feature.stats.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment // ✅ Thêm import này
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun TimeRangeSelector(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("Ngày", "Tuần", "Tháng", "Năm")

    val indicatorBias by animateFloatAsState(
        targetValue = when (selectedTab) {
            0 -> -1f      // Sát bên trái (Ngày)
            1 -> -0.33f   // 1/3 bên trái (Tuần)
            2 -> 0.33f    // 1/3 bên phải (Tháng)
            else -> 1f    // Sát bên phải (Năm)
        },
        label = "TabIndicatorAnimation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Hàng chứa các chữ tiêu đề tab
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            tabs.forEachIndexed { index, text ->
                val isActive = selectedTab == index

                Box(
                    modifier = Modifier
                        .weight(1f) // Chia đều 4 phần bằng nhau bằng Weight
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTabSelected(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        style = if (isActive) StatTabActiveStyle2 else StatTabInactiveStyle2,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            contentAlignment = BiasAlignment(horizontalBias = indicatorBias, verticalBias = 0f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(0.25f),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .background(
                            color = MintGreen,
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                        )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimeRangeSelectorPreview() {
    var selectedTab by remember { mutableStateOf(1) }
    FoodTrackerTheme {
        TimeRangeSelector(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }
}