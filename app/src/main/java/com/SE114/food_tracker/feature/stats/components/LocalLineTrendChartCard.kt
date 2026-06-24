package com.SE114.food_tracker.feature.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import kotlinx.datetime.LocalDate

@Composable
fun LocalLineTrendChartCard(
    title: String,
    forecast: TrendForecast,
    budgetLimit: Double?,
    timeFrame: TimeFrame,
    anchorDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val points = forecast.points
    val axisLabels = listOf("Kỳ trước", "Hiện tại", "Dự báo cuối kỳ")
    val isExceeded = budgetLimit != null && forecast.projectedTotal > budgetLimit

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
                        text = LocalCurrencyDisplay.current.formatShort(forecast.currentActual),
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
                        text = LocalCurrencyDisplay.current.formatShort(forecast.projectedTotal),
                        fontSize = 18.sp,
                        color = if (isExceeded) AlertRed else StatPinkDark,
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

                drawLine(
                    color = ChartLineObserved,
                    start = Offset(xFor(0), yFor(points[0])),
                    end = Offset(xFor(1), yFor(points[1])),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )

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
                        color = if (isExceeded) AlertRed else StatPinkDark,
                        start = Offset(currentX, currentY),
                        end = Offset(currentX + stepX * 0.5f, currentY + stepY * 0.5f),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                    currentX += stepX
                    currentY += stepY
                }

                points.forEachIndexed { i, value ->
                    val markerColor = if (i == 2) (if (isExceeded) AlertRed else StatPinkDark) else ChartMarkerNormal
                    drawCircle(
                        color = markerColor,
                        radius = 6f,
                        center = Offset(xFor(i), yFor(value))
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                axisLabels.forEachIndexed { i, label ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = if (i == 2) (if (isExceeded) AlertRed else StatPinkDark) else TextLabelGray,
                            fontWeight = if (i == 2) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = LocalCurrencyDisplay.current.formatShort(points[i].toDouble()),
                            fontSize = 11.sp,
                            color = TextPrimaryStat
                        )
                    }
                }
            }

            // ── ✅ DYNAMIC ALERT BOX (Đã sửa thuật toán đa chu kỳ thích ứng) ──────────
            if (isExceeded && budgetLimit != null) {
                val remaining = forecast.remainingCycles
                val cycleAverage = if (remaining > 0) {
                    (forecast.projectedTotal - forecast.currentActual) / remaining
                } else {
                    0.0
                }

                // Xác định vị trí mốc thời gian hiện tại trong chu kỳ nền
                val currentUnitIndex = when (timeFrame) {
                    TimeFrame.WEEK  -> anchorDate.dayOfWeek.ordinal + 1 // Trả về 1..7 (Thứ 2 -> Chủ Nhật)
                    TimeFrame.MONTH -> anchorDate.dayOfMonth            // Trả về 1..31
                    TimeFrame.YEAR  -> anchorDate.monthNumber           // Trả về 1..12
                    TimeFrame.DAY   -> 1
                }

                val alertText = if (forecast.currentActual > budgetLimit) {
                    // Trường hợp 1: Ngay lúc này chi tiêu thực tế đã vượt ngân sách đặt ra
                    when (timeFrame) {
                        TimeFrame.WEEK  -> "Chi tiêu thực tế hiện tại đã vượt quá giới hạn ngân sách tuần này!"
                        TimeFrame.MONTH -> "Chi tiêu thực tế hiện tại đã vượt quá giới hạn ngân sách tháng này!"
                        TimeFrame.YEAR  -> "Chi tiêu thực tế hiện tại đã vượt quá giới hạn ngân sách năm nay!"
                        else            -> "Chi tiêu thực tế hiện tại đã vượt quá ngân sách hôm nay!"
                    }
                } else {
                    // Trường hợp 2: Hiện tại chưa vượt, nhưng đà dự báo tương lai sẽ vượt. Mô phỏng tìm điểm gãy.
                    var accumulated = forecast.currentActual
                    var depletionUnit = currentUnitIndex
                    for (i in 1..remaining) {
                        accumulated += cycleAverage
                        if (accumulated > budgetLimit) {
                            depletionUnit = currentUnitIndex + i
                            break
                        }
                    }

                    when (timeFrame) {
                        TimeFrame.WEEK -> {
                            val dayNames = listOf("Hai", "Ba", "Tư", "Năm", "Sáu", "Bảy", "Chủ Nhật")
                            val dayName = dayNames.getOrNull((depletionUnit - 1).coerceIn(0, 6)) ?: ""
                            "Đà này thì đến ngày thứ $depletionUnit trong tuần (Thứ $dayName) là bạn sẽ hết sạch tiền ăn tuần này."
                        }
                        TimeFrame.MONTH -> {
                            "Đà này thì đến ngày $depletionUnit của tháng là bạn sẽ hết sạch tiền ăn tháng này."
                        }
                        TimeFrame.YEAR -> {
                            "Đà này thì đến Tháng $depletionUnit là bạn sẽ hết sạch tiền ăn năm nay."
                        }
                        else -> "Đà này bạn sẽ sớm chi vượt ngân sách đặt ra."
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = AlertRedBg, shape = RoundedCornerShape(12.dp))
                        .border(width = 1.dp, color = AlertRedBorder, shape = RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Vượt ngân sách",
                        tint = AlertRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alertText,
                        color = AlertRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = AlertGreenBg, shape = RoundedCornerShape(12.dp))
                        .border(width = 1.dp, color = AlertGreenBorder, shape = RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Trong ngân sách",
                        tint = AlertGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tốt lắm! Dự kiến tiêu trong ngân sách.",
                        color = AlertGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
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
                    previousTotal = 162_000.0,
                    currentActual = 302_000.0,
                    projectedTotal = 417_000.0,
                    remainingCycles = 4
                ),
                budgetLimit = 350_000.0,
                timeFrame = TimeFrame.WEEK,
                anchorDate = LocalDate(2026, 6, 24)
            )
        }
    }
}