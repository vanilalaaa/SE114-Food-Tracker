package com.SE114.food_tracker.feature.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.core.util.LocalCurrencyDisplay
import com.SE114.food_tracker.core.util.TimeFrame
import com.SE114.food_tracker.feature.stats.TrendForecast
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

private data class ForecastDisplayData(
    val projectedTotal: Double,
    val currentActual: Double,
    val budgetFraction: Float,
    val projectedFraction: Float,
    val rateLabel: String,
    val elapsedLabel: String,
    val depletionLabel: String?,
    val isOverBudget: Boolean
)

@Composable
private fun rememberForecastDisplayData(
    forecast: TrendForecast,
    budgetLimit: Double?,
    timeFrame: TimeFrame,
    anchorDate: LocalDate
): ForecastDisplayData {
    val currency = LocalCurrencyDisplay.current

    return remember(forecast, budgetLimit, timeFrame, anchorDate) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val daysInMonth = run {
            val nextMonth = if (anchorDate.monthNumber == 12) 1 else anchorDate.monthNumber + 1
            val nextYear  = if (anchorDate.monthNumber == 12) anchorDate.year + 1 else anchorDate.year
            LocalDate(nextYear, nextMonth, 1).toEpochDays() -
                    LocalDate(anchorDate.year, anchorDate.monthNumber, 1).toEpochDays()
        }.toInt()

        // ── Đã sửa: Tính toán elapsed chính xác dựa trên việc so sánh quá khứ/hiện tại/tương lai ──
        val (elapsed, total, unitSingular, unitPlural) = when (timeFrame) {
            TimeFrame.DAY -> {
                val elapsedHours = when {
                    anchorDate > today -> 0
                    anchorDate < today -> 24
                    else -> Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour.coerceAtLeast(1)
                }
                Quad(elapsedHours, 24, "giờ", "giờ")
            }
            TimeFrame.WEEK -> {
                val weekStart = anchorDate - DatePeriod(days = anchorDate.dayOfWeek.isoDayNumber - 1)
                val weekEnd   = weekStart + DatePeriod(days = 6)
                val elapsedDays = when {
                    today < weekStart -> 0
                    today > weekEnd   -> 7
                    else -> (today.toEpochDays() - weekStart.toEpochDays() + 1).toInt()
                }
                Quad(elapsedDays, 7, "ngày", "ngày")
            }
            TimeFrame.MONTH -> {
                val elapsedDays = when {
                    anchorDate.year > today.year || (anchorDate.year == today.year && anchorDate.monthNumber > today.monthNumber) -> 0
                    anchorDate.year < today.year || (anchorDate.year == today.year && anchorDate.monthNumber < today.monthNumber) -> daysInMonth
                    else -> today.dayOfMonth
                }
                Quad(elapsedDays, daysInMonth, "ngày", "ngày")
            }
            TimeFrame.YEAR -> {
                val elapsedMonths = when {
                    anchorDate.year > today.year -> 0
                    anchorDate.year < today.year -> 12
                    else -> today.monthNumber
                }
                Quad(elapsedMonths, 12, "tháng", "tháng")
            }
        }

        val elapsedSafe = elapsed.coerceAtLeast(1)

        // ── Tốc độ chi tiêu dựa trên thực tế trôi qua ──
        val ratePerUnit = forecast.currentActual / elapsedSafe

        // ── Đã sửa: Sử dụng giá trị dự báo từ ViewModel làm Single Source of Truth ──
        val projectedTotal = forecast.projectedTotal

        // ── Tỷ lệ phần trăm hiển thị trên biểu đồ vòng tròn ──
        val budgetFraction = if (budgetLimit != null && budgetLimit > 0) {
            (forecast.currentActual / budgetLimit).toFloat().coerceIn(0f, 1f)
        } else 0f

        val projectedFraction = if (budgetLimit != null && budgetLimit > 0) {
            (projectedTotal / budgetLimit).toFloat().coerceIn(0f, 1f)
        } else 0f

        val rateLabel = "${currency.formatShort(ratePerUnit)}/$unitSingular"
        val elapsedLabel = "$elapsed/$total $unitPlural đã qua"

        val isOverBudget = budgetLimit != null && projectedTotal > budgetLimit
        val depletionLabel: String? = if (isOverBudget && budgetLimit != null && ratePerUnit > 0) {
            val depletionUnit = Math.ceil(budgetLimit / ratePerUnit).toInt()
            when (timeFrame) {
                TimeFrame.DAY -> {
                    if (depletionUnit <= 24) "Khoảng ${depletionUnit}h sẽ hết ngân sách hôm nay" else null
                }
                TimeFrame.WEEK -> {
                    val dayNames = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                    val name = dayNames.getOrNull((depletionUnit - 1).coerceIn(0, 6)) ?: ""
                    if (depletionUnit <= 7) "Đến $name tuần này sẽ hết ngân sách"
                    else "Dự kiến tiêu ${currency.formatShort(projectedTotal)} — vượt ngân sách tuần"
                }
                TimeFrame.MONTH -> {
                    if (depletionUnit <= daysInMonth) "Đến ngày $depletionUnit/$daysInMonth sẽ hết ngân sách tháng"
                    else "Dự kiến tiêu ${currency.formatShort(projectedTotal)} — vượt ngân sách tháng"
                }
                TimeFrame.YEAR -> {
                    if (depletionUnit <= 12) "Đến Tháng $depletionUnit sẽ hết ngân sách năm"
                    else "Dự kiến tiêu ${currency.formatShort(projectedTotal)} — vượt ngân sách năm"
                }
            }
        } else null

        ForecastDisplayData(
            projectedTotal    = projectedTotal,
            currentActual     = forecast.currentActual,
            budgetFraction    = budgetFraction,
            projectedFraction = projectedFraction,
            rateLabel         = rateLabel,
            elapsedLabel      = elapsedLabel,
            depletionLabel    = depletionLabel,
            isOverBudget      = isOverBudget
        )
    }
}

@Composable
fun ForecastCard(
    forecast: TrendForecast,
    budgetLimit: Double?,
    timeFrame: TimeFrame,
    anchorDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val currency = LocalCurrencyDisplay.current
    val data = rememberForecastDisplayData(forecast, budgetLimit, timeFrame, anchorDate)

    val ringColor    = if (data.isOverBudget) AlertRed     else AlertGreen
    val ringBgColor  = if (data.isOverBudget) AlertRedBg   else AlertGreenBg
    val accentColor  = if (data.isOverBudget) AlertRed     else StatPinkDark

    Card(
        modifier  = modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        shape     = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Dự báo",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = TextPrimaryStat
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Dự kiến cuối kỳ",
                        fontSize = 13.sp,
                        color    = TextLabelGray
                    )
                    Text(
                        currency.formatShort(data.projectedTotal),
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = accentColor
                    )
                    if (budgetLimit != null) {
                        Text(
                            "Ngân sách: ${currency.formatShort(budgetLimit)}",
                            modifier = Modifier.padding(top = 8.dp),
                            fontSize = 13.sp,
                            color    = TextLabelGray
                        )
                    }
                }

                if (budgetLimit != null) {
                    val pct = (data.projectedFraction * 100).toInt().coerceIn(0, 999)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier.size(80.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 9.dp.toPx()
                            val inset  = stroke / 2f
                            val arcSize = Size(size.width - stroke, size.height - stroke)

                            drawArc(
                                color      = Color(0xFFEEEEEE),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter  = false,
                                topLeft    = Offset(inset, inset),
                                size       = arcSize,
                                style      = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color      = ringColor,
                                startAngle = -90f,
                                sweepAngle = 360f * data.projectedFraction,
                                useCenter  = false,
                                topLeft    = Offset(inset, inset),
                                size       = arcSize,
                                style      = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            "$pct%",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = ringColor
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = ringBgColor, shape = RoundedCornerShape(12.dp))
                    .border(width = 1.dp, color = CardWhite, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = if (data.isOverBudget) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint               = ringColor,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = data.depletionLabel
                        ?: if (data.isOverBudget) "Dự kiến vượt ngân sách kỳ này!"
                        else "Tốt lắm! Dự kiến tiêu trong ngân sách",
                    color      = ringColor,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tốc độ:  ",
                    fontSize = 12.sp,
                    color    = TextLabelGray
                )
                Text(
                    data.rateLabel,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimaryStat
                )
                Text(
                    "  ·  ${data.elapsedLabel}",
                    fontSize = 12.sp,
                    color    = TextLabelGray
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
private operator fun <A, B, C, D> Quad<A, B, C, D>.component1() = first
private operator fun <A, B, C, D> Quad<A, B, C, D>.component2() = second
private operator fun <A, B, C, D> Quad<A, B, C, D>.component3() = third
private operator fun <A, B, C, D> Quad<A, B, C, D>.component4() = fourth