package com.SE114.food_tracker.feature.stats

import com.SE114.food_tracker.core.util.TimeFrame
import kotlinx.datetime.LocalDate

enum class ContentTab { EXPENSE, ANALYSIS }

// ─── Chart-ready value objects (no Vico imports — TV4 maps these to Vico models) ─

/** One bar in a bar chart. [label] is already localised (e.g. "T2", "Th3", "Sáng"). */
data class ChartBar(val label: String, val value: Double)

/**
 * One slice in a donut chart.
 * [iconUrl] is the category emoji/icon stored in Room — TV4 uses it to render the legend.
 */
data class ChartSlice(
    val categoryId: String,
    val label: String,
    val value: Double,
    val iconUrl: String
)

/** Top-N category insight card row. */
data class CategoryStat(
    val name: String,
    val iconUrl: String,
    val total: Double
)

/** "Phổ biến nhất" card row. */
data class PopularFoodStat(
    val name: String,
    val recordCount: Int,
    val totalSpent: Double
)

/**
 * "Wallet Destroyer" — the single most expensive item in the current period.
 * Exposes just enough fields for the UI card; TV4 does not need the full Item entity.
 */
data class WalletDestroyerItem(
    val itemId: String,
    val name: String,
    val categoryName: String,
    val categoryIconUrl: String,
    val price: Double,
    val currencyCode: String,
    val imageUrl: String?
)

// ─── Budget ──────────────────────────────────────────────────────────────────

/**
 * Computed budget state for the currently-selected [TimeFrame].
 * [limit] is null when the user has not set a budget for that period.
 */
data class BudgetUiState(
    val daily: Double? = null,
    val weekly: Double? = null,
    val monthly: Double? = null,
    val yearly: Double? = null,
    // Derived for the active time frame:
    val limit: Double? = null,
    val spent: Double = 0.0
) {
    val remaining: Double get() = (limit ?: 0.0) - spent
    val isExceeded: Boolean get() = limit != null && spent > limit
    val hasLimit: Boolean get() = limit != null
}

// ─── Summary ─────────────────────────────────────────────────────────────────

data class StatisticsSummary(
    val totalSpent: Double = 0.0,
    val itemCount: Int = 0,
    val previousPeriodTotal: Double = 0.0
) {
    /** Signed percentage change vs previous period. 0.0 when previous period had no spend. */
    val percentChange: Double
        get() = if (previousPeriodTotal == 0.0) 0.0
        else (totalSpent - previousPeriodTotal) / previousPeriodTotal * 100.0

    val isIncrease: Boolean get() = percentChange > 0.0
}

// ─── Root UiState ─────────────────────────────────────────────────────────────

data class StatisticsUiState(
    // ── Navigation / selection ──
    val timeFrame: TimeFrame = TimeFrame.DAY,
    val contentTab: ContentTab = ContentTab.EXPENSE,
    val anchorDate: LocalDate? = null,
    val headerLabel: String = "",

    // ── Summary row ──
    val summary: StatisticsSummary = StatisticsSummary(),

    // ── Budget ──
    val budget: BudgetUiState = BudgetUiState(),

    // ── Charts ──
    val barChartData: List<ChartBar> = emptyList(),
    val donutData: List<ChartSlice> = emptyList(),

    // ── Insight cards ──
    val topCategories: List<CategoryStat> = emptyList(),
    val walletDestroyer: WalletDestroyerItem? = null,  // top-1
    val popularFoods: List<PopularFoodStat> = emptyList(),

    // ── Analysis tab ──
    val trendPoints: List<Float> = emptyList(),

    // ── Async ──
    val isLoading: Boolean = true,
    val error: String? = null
)