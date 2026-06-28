package com.SE114.food_tracker.feature.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.core.util.*

@Composable
fun ChartItem(
    label: String,
    value: Double,
    maxHeight: Int,
    maxValue: Double,
    isHighest: Boolean,
    modifier: Modifier = Modifier
) {
    val barHeight = if (value > 0 && maxValue > 0) (value / maxValue * maxHeight).dp else 4.dp
    val barColor = when {
        value == 0.0 -> Color.White.copy(alpha = 0.35f)
        isHighest -> StatPinkDark
        else -> Color.White
    }

    Column(
        // Sử dụng weight(1f) thay vì width cố định để các cột tự chia đều khoảng trống vừa khít với màn hình
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (value > 0) {
            Text(
                text = LocalCurrencyDisplay.current.formatShort(value),
                style = StatLabelStyle,
                color = TextPrimaryStat,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Box(modifier = Modifier.height(20.dp))
        }

        Box(
            modifier = Modifier
                .width(20.dp) // độ rộng của thanh (Bar)
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
            color = TextPrimaryStat,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun ChartCard(
    title: String,
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val targetSessions = listOf("Sáng", "Trưa", "Chiều", "Tối")
    val isSessionChart = title.contains("giờ", ignoreCase = true) || title.contains("buổi", ignoreCase = true)

    val finalizedData = if (isSessionChart) {
        targetSessions.map { targetLabel ->
            val matchingValue = data.find {
                it.first.equals(targetLabel, ignoreCase = true)
            }?.second ?: 0.0
            targetLabel to matchingValue
        }
    } else {
        data
    }

    val maxValue = finalizedData.maxOfOrNull { it.second } ?: 0.0
    val chartBaseHeight = 100
    val calendarHighlight = Color(0xFFFCDFCF)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = calendarHighlight,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 12.dp) // padding ngang của card
        ) {
            Text(
                text = title,
                style = StatSectionTitleStyle,
                color = TextPrimaryStat,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((chartBaseHeight + 40).dp), // Khóa chiều cao cố định cho hàng biểu đồ
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                finalizedData.forEach { (label, value) ->
                    val isHighest = value == maxValue && maxValue > 0
                    ChartItem(
                        label = label,
                        value = value,
                        maxHeight = chartBaseHeight,
                        maxValue = maxValue,
                        isHighest = isHighest,
                        modifier = Modifier.weight(1f) // Ép các cột tự động co giãn đều nhau vừa vặn trong Row
                    )
                }
            }
        }
    }
}