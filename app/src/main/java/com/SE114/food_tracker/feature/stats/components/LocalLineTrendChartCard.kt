package com.SE114.food_tracker.feature.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.feature.stats.TrendForecast
import com.SE114.food_tracker.core.util.*

@Composable
fun LocalLineTrendChartCard(
    title: String,
    forecast: TrendForecast,
    modifier: Modifier = Modifier
) {
    val points = forecast.points
    val axisLabels = listOf("Kỳ trước", "Hiện tại", "Dự báo cuối kỳ")

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

            // ── Headline numbers: current actual vs projected end-of-period ──────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "CHI TIÊU THỰC TẾ",
                        fontSize = 10.sp,
                        color = TextLabelGray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${forecast.currentActual.formatVndShort()} đ",
                        fontSize = 18.sp,
                        color = TextPrimaryStat,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "DỰ BÁO CUỐI KỲ",
                        fontSize = 10.sp,
                        color = TextLabelGray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${forecast.projectedTotal.formatVndShort()} đ",
                        fontSize = 18.sp,
                        color = StatPinkDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Canvas(modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)) {
                if (points.size < 3) return@Canvas

                val widthInterval = size.width / (points.size - 1)
                val maxVal = (points.maxOrNull() ?: 0f).coerceAtLeast(1f)
                val chartHeight = size.height * 0.75f

                fun xFor(i: Int) = i * widthInterval
                fun yFor(value: Float) = size.height - (value / maxVal * chartHeight)

                // ── Segment 0: previous → current actual (solid, observed) ──────────
                drawLine(
                    color = ChartLineObserved,
                    start = Offset(xFor(0), yFor(points[0])),
                    end = Offset(xFor(1), yFor(points[1])),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )

                // ── Segment 1: current actual → projected (dashed, forecast) ────────
                val startX = xFor(1)
                val startY = yFor(points[1])
                val endX = xFor(2)
                val endY = yFor(points[2])
                val dashLength = 10f
                val gapLength = 10f
                val deltaX = endX - startX
                val deltaY = endY - startY
                val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
                val segments = (distance / (dashLength + gapLength)).toInt().coerceAtLeast(1)
                val stepX = deltaX / segments
                val stepY = deltaY / segments
                var currentX = startX
                var currentY = startY
                repeat(segments) {
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

                // ── Point markers at all 3 nodes ─────────────────────────────────────
                points.forEachIndexed { i, value ->
                    val markerColor = if (i == 2) StatPinkDark else ChartMarkerNormal
                    drawCircle(
                        color = markerColor,
                        radius = 6f,
                        center = Offset(xFor(i), yFor(value))
                    )
                }
            }

            // ── Axis labels under each node ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                axisLabels.forEachIndexed { i, label ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = if (i == 2) StatPinkDark else TextLabelGray,
                            fontWeight = if (i == 2) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "${points[i].toDouble().formatVndShort()} đ",
                            fontSize = 11.sp,
                            color = TextPrimaryStat
                        )
                    }
                }
            }

            Text(
                text = "👉 Đường nét đứt màu hồng biểu thị xu hướng dự báo tuyến tính kỳ này" +
                        if (forecast.remainingCycles > 0) " (còn ${forecast.remainingCycles} kỳ con)." else ".",
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
                forecast = TrendForecast(
                    previousTotal = 850_000.0,
                    currentActual = 420_000.0,
                    projectedTotal = 910_000.0,
                    remainingCycles = 12
                )
            )
        }
    }
}