package com.SE114.food_tracker.feature.stats

import com.SE114.food_tracker.core.util.TimeFrame
import kotlinx.datetime.LocalDate

enum class ContentTab { EXPENSE, ANALYSIS }

data class ChartBar(val label: String, val value: Double)

data class ChartSlice(
    val categoryId: String,
    val label: String,
    val value: Double,
    val iconUrl: String
)

data class CategoryStat(
    val name: String,
    val iconUrl: String,
    val total: Double
)

data class WalletDestroyerItem(
    val itemId: String,
    val name: String,
    val categoryName: String,
    val categoryIconUrl: String,
    val price: Double,
    val currencyCode: String,
    val imageUrl: String?,
    val recordCount: Int = 1,
    val percentageShare: Double = 0.0  // (price * recordCount) / totalSpent * 100
)

// ─── Detail list ─────────────────────────────────────────────────────────────

/**
 * One flattened meal log row for [DetailCardSection].
 * [timeLabel]      → "Sáng" / "Trưa" / "Tối"
 * [dateLabel]      → "Thứ Sáu, 03/06"  (used as DayGroup header in weekly+ mode)
 * [entryDateEpoch] → raw epoch millis for grouping
 */
data class DetailItem(
    val itemId: String,
    val name: String,
    val categoryName: String,
    val categoryIconUrl: String,
    val price: Double,
    val currencyCode: String,
    val timeLabel: String,
    val dateLabel: String,
    val entryDateEpoch: Long,
    val imageUrl: String?
)

// ─── Budget ───────────────────────────────────────────────────────────────────

data class BudgetUiState(
    val daily: Double? = null,
    val weekly: Double? = null,
    val monthly: Double? = null,
    val yearly: Double? = null,
    val limit: Double? = null,
    val spent: Double = 0.0
) {
    val remaining: Double  get() = (limit ?: 0.0) - spent
    val isExceeded: Boolean get() = limit != null && spent > limit
    val hasLimit: Boolean   get() = limit != null
}

// ─── Summary ─────────────────────────────────────────────────────────────────

data class StatisticsSummary(
    val totalSpent: Double = 0.0,
    val itemCount: Int = 0,
    val previousPeriodTotal: Double = 0.0
) {
    val percentChange: Double
        get() = if (previousPeriodTotal == 0.0) 0.0
        else (totalSpent - previousPeriodTotal) / previousPeriodTotal * 100.0

    val isIncrease: Boolean get() = percentChange > 0.0
}

// ─── Trend forecast (Sprint 3) ─────────────────────────────────────────────

/**
 * Three-point forecast for [LocalLineTrendChartCard]:
 *  - [previousTotal]  → total spend in the period immediately before the active one (solid, past)
 *  - [currentActual]  → real-time spend so far in the active period (solid, past)
 *  - [projectedTotal] → forecasted end-of-period total (dashed, future)
 *
 * [projectedTotal] = currentActual + (historicalCycleAverage * remainingCycles).
 * For [com.SE114.food_tracker.core.util.TimeFrame.DAY] there is no sub-cycle to project
 * into, so [remainingCycles] is always 0 and [projectedTotal] equals [currentActual].
 */
data class TrendForecast(
    val previousTotal: Double = 0.0,
    val currentActual: Double = 0.0,
    val projectedTotal: Double = 0.0,
    val remainingCycles: Int = 0
) {
    /** Convenience list for chart code that just wants the 3 raw Y-values. */
    val points: List<Float>
        get() = listOf(previousTotal.toFloat(), currentActual.toFloat(), projectedTotal.toFloat())
}

// ─── Root UiState ─────────────────────────────────────────────────────────────

data class StatisticsUiState(
    // Navigation
    val timeFrame: TimeFrame = TimeFrame.DAY,
    val contentTab: ContentTab = ContentTab.EXPENSE,
    val anchorDate: LocalDate? = null,
    val headerLabel: String = "",
    val datesWithData: List<Int> = emptyList(),

    // Summary row
    val summary: StatisticsSummary = StatisticsSummary(),

    // Budget
    val budget: BudgetUiState = BudgetUiState(),

    // Charts
    val barChartData: List<ChartBar> = emptyList(),
    val donutData: List<ChartSlice> = emptyList(),

    // Insight cards
    val topCategories: List<CategoryStat> = emptyList(),
    val walletDestroyer: WalletDestroyerItem? = null,

    // Detail meal log list (CHI TIẾT section)
    val detailItems: List<DetailItem> = emptyList(),

    // Analysis tab
    val trendForecast: TrendForecast = TrendForecast(),

    // Async
    val isLoading: Boolean = true,
    val error: String? = null
)