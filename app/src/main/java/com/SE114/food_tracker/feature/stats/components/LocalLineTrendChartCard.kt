package com.SE114.food_tracker.feature.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun LocalLineTrendChartCard(
    title: String,
    points: List<Float>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = StatSectionTitleStyle,
                color = TextPrimaryStat,
                fontSize = 14.sp
            )

            Canvas(modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)) {
                val widthInterval = size.width / (points.size - 1)
                val maxVal = points.maxOrNull() ?: 100f

                for (i in 0 until points.size - 1) {
                    val startX = i * widthInterval
                    val startY = size.height - (points[i] / maxVal * size.height * 0.75f)
                    val endX = (i + 1) * widthInterval
                    val endY = size.height - (points[i + 1] / maxVal * size.height * 0.75f)

                    if (i == points.size - 2) {
                        val dashLength = 10f
                        val gapLength = 10f
                        var currentX = startX
                        var currentY = startY
                        val deltaX = endX - startX
                        val deltaY = endY - startY
                        val distance =
                            Math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toFloat()
                        val segments = (distance / (dashLength + gapLength)).toInt()

                        val stepX = deltaX / segments
                        val stepY = deltaY / segments

                        for (j in 0 until segments) {
                            drawLine(
                                color = StatPinkDark,
                                start = Offset(currentX, currentY),
                                end = Offset(currentX + stepX * 0.5f, currentY + stepY * 0.5f),
                                strokeWidth = 5f,
                                cap = StrokeCap.Round
                            )
                            currentX += stepX
                            currentY += stepY
                        }
                    } else {
                        drawLine(
                            color = Color(0xFFAED9E0),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            Text(
                text = "👉 Đường nét đứt màu hồng biểu thị xu hướng dự báo tuyến tính kỳ sau.",
                fontSize = 11.sp,
                color = TextLabelGray
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LocalLineTrendChartCardPreview() {
    FoodTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MainBackground)
                .padding(16.dp)
        ) {
            LocalLineTrendChartCard(
                title = "Dự báo xu hướng kỳ tới",
                points = listOf(45f, 70f, 110f, 130f)
            )
        }
    }
}