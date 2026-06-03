package com.SE114.food_tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun ChartItem(
    label: String,
    value: Int,
    maxHeight: Int,
    maxValue: Int,
    isHighest: Boolean,
    modifier: Modifier = Modifier
) {
    val barHeight = if (maxValue > 0) (value.toFloat() / maxValue * maxHeight).dp else 0.dp
    val barColor = if (isHighest) StatPinkDark else Color.White

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (value > 0) {
            Text(
                text = value.toString(),
                style = StatLabelStyle,
                color = TextPrimaryStat
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Box(
            modifier = Modifier
                .width(28.dp)
                .height(barHeight)
                .background(
                    color = barColor,
                    shape = RoundedCornerShape(4.dp)
                )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = StatLabelStyle,
            color = TextPrimaryStat
        )
    }
}

@Composable
fun ChartCard(
    title: String,
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { it.second } ?: 0
    val chartBaseHeight = 100
    val calendarHighlight = Color(0xFFFCDFCF)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = calendarHighlight,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                style = StatSectionTitleStyle,
                color = TextPrimaryStat
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { (label, value) ->
                    val isHighest = value == maxValue && maxValue > 0
                    ChartItem(
                        label = label,
                        value = value,
                        maxHeight = chartBaseHeight,
                        maxValue = maxValue,
                        isHighest = isHighest
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChartCardPreview() {
    val sampleData = listOf(
        "Sáng" to 1,
        "Trưa" to 1,
        "Chiều" to 3,
        "Tối" to 1,
        "Khuya" to 1
    )
    FoodTrackerTheme {
        Box(modifier = Modifier.padding(16.dp).background(MainBackground)) {
            ChartCard(
                title = "Món theo buổi",
                data = sampleData
            )
        }
    }
}