package com.SE114.food_tracker.feature.stats.components

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
import androidx.compose.foundation.Canvas
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.core.util.LocalCurrencyDisplay
import com.SE114.food_tracker.core.util.TimeFrame
import com.SE114.food_tracker.feature.stats.TrendForecast
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

// ── Data class describing all derived forecast values ─────────────────────────
private data class ForecastDisplayData(
    val projectedTotal: Double,
    val currentActual: Double,
    val budgetFraction: Float,      // currentActual / budget (capped at 1f for ring)
    val projectedFraction: Float,   // projectedTotal / budget (capped at 1f for ring)
    val rateLabel: String,          // e.g. "6,666 đ/ngày"
    val elapsedLabel: String,       // e.g. "3/7 ngày đã qua"
    val depletionLabel: String?,    // non-null when projected > budget
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
        // ── 1. Elapsed & total units in the active period ──────────────────
        val currentHour = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .hour
            .coerceAtLeast(1)

        val daysInMonth = run {
            val nextMonth = if (anchorDate.monthNumber == 12) 1 else anchorDate.monthNumber + 1
            val nextYear  = if (anchorDate.monthNumber == 12) anchorDate.year + 1 else anchorDate.year
            LocalDate(nextYear, nextMonth, 1).toEpochDays() -
                    LocalDate(anchorDate.year, anchorDate.monthNumber, 1).toEpochDays()
        }.toInt()

        val (elapsed, total, unitSingular, unitPlural) = when (timeFrame) {
            TimeFrame.DAY   -> Quad(currentHour,                        24,           "giờ",   "giờ")
            TimeFrame.WEEK  -> Quad(anchorDate.dayOfWeek.isoDayNumber,  7,            "ngày",  "ngày")
            TimeFrame.MONTH -> Quad(anchorDate.dayOfMonth,              daysInMonth,  "ngày",  "ngày")
            TimeFrame.YEAR  -> Quad(anchorDate.monthNumber,             12,           "tháng", "tháng")
        }

        val elapsedSafe = elapsed.coerceAtLeast(1)

        // ── 2. Spend rate per unit ─────────────────────────────────────────
        val ratePerUnit = forecast.currentActual / elapsedSafe

        // ── 3. Projected total using live rate (overrides ViewModel value when budget present) ──
        val projectedTotal = ratePerUnit * total

        // ── 4. Budget fractions for the ring ──────────────────────────────
        val budgetFraction = if (budgetLimit != null && budgetLimit > 0) {
            (forecast.currentActual / budgetLimit).toFloat().coerceIn(0f, 1f)
        } else 0f

        val projectedFraction = if (budgetLimit != null && budgetLimit > 0) {
            (projectedTotal / budgetLimit).toFloat().coerceIn(0f, 1f)
        } else 0f

        // ── 5. Labels ──────────────────────────────────────────────────────
        val rateLabel = "${currency.formatShort(ratePerUnit)}/$unitSingular"
        val elapsedLabel = "$elapsed/$total $unitPlural đã qua"

        // ── 6. Depletion label (only when projected > budget) ──────────────
        val isOverBudget = budgetLimit != null && projectedTotal > budgetLimit
        val depletionLabel: String? = if (isOverBudget && budgetLimit != null && ratePerUnit > 0) {
            val depletionUnit = Math.ceil(budgetLimit / ratePerUnit).toInt()
            when (timeFrame) {
                TimeFrame.DAY -> {
                    if (depletionUnit <= 24) "Khoảng ${depletionUnit}h sẽ hết ngân sách hôm nay"
                    else null
                }
                TimeFrame.WEEK -> {
                    val dayNames = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                    val name = dayNames.getOrNull((depletionUnit - 1).coerceIn(0, 6)) ?: ""
                    if (depletionUnit <= 7) "Đến $name ngày $depletionUnit/7 sẽ hết ngân sách tuần"
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
    val ringBorder   = if (data.isOverBudget) AlertRedBorder else AlertGreenBorder
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

            // ── Header row: title + ring ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title block
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

                // Circular ring
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

                            // Background track
                            drawArc(
                                color      = Color(0xFFEEEEEE),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter  = false,
                                topLeft    = Offset(inset, inset),
                                size       = arcSize,
                                style      = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            // Progress arc (projected fraction)
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

            // ── Status banner ─────────────────────────────────────────────
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

            // ── Speed footer ──────────────────────────────────────────────
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

// ── Small tuple helper ────────────────────────────────────────────────────────
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private operator fun <A, B, C, D> Quad<A, B, C, D>.component1() = first
private operator fun <A, B, C, D> Quad<A, B, C, D>.component2() = second
private operator fun <A, B, C, D> Quad<A, B, C, D>.component3() = third
private operator fun <A, B, C, D> Quad<A, B, C, D>.component4() = fourth

// ── Preview ───────────────────────────────────────────────────────────────────
@Preview(showBackground = true)
@Composable
fun ForecastCardPreview_UnderBudget() {
    FoodTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MainBackground)
                .padding(16.dp)
        ) {
            ForecastCard(
                forecast = TrendForecast(
                    previousTotal   = 120_000.0,
                    currentActual   = 20_000.0,
                    projectedTotal  = 46_666.0,
                    remainingCycles = 4
                ),
                budgetLimit = 500_000.0,
                timeFrame   = TimeFrame.WEEK,
                anchorDate  = LocalDate(2026, 6, 24)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ForecastCardPreview_OverBudget() {
    FoodTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MainBackground)
                .padding(16.dp)
        ) {
            ForecastCard(
                forecast = TrendForecast(
                    previousTotal   = 200_000.0,
                    currentActual   = 280_000.0,
                    projectedTotal  = 650_000.0,
                    remainingCycles = 2
                ),
                budgetLimit = 500_000.0,
                timeFrame   = TimeFrame.WEEK,
                anchorDate  = LocalDate(2026, 6, 24)
            )
        }
    }
}