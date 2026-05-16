package com.SE114.food_tracker.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.ui.theme.*

@Composable
fun TimeRangeSelector(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("Ngày", "Tuần", "Tháng", "Năm")

    val indicatorOffset by animateDpAsState(
        targetValue = when (selectedTab) {
            0 -> 6.dp
            1 -> 64.dp
            2 -> 122.dp
            else -> 183.dp
        },
        label = "TabIndicatorAnimation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            tabs.forEachIndexed { index, text ->
                val isActive = selectedTab == index

                Text(
                    text = text,
                    style = if (isActive) StatTabActiveStyle else StatTabInactiveStyle,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTabSelected(index)
                        }
                        .padding(vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .padding(start = indicatorOffset)
                .width(24.dp)
                .height(3.dp)
                .background(
                    color = MintGreen,
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                )
        )
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