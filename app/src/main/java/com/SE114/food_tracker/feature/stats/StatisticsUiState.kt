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

data class PopularFoodStat(
    val name: String,
    val recordCount: Int,
    val totalSpent: Double,
    val imageUrl: String? = null,
    val categoryIconUrl: String = "🍽️"
)

data class WalletDestroyerItem(
    val itemId: String,
    val name: String,
    val categoryName: String,
    val categoryIconUrl: String,
    val price: Double,
    val currencyCode: String,
    val imageUrl: String?
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

// ─── Root UiState ─────────────────────────────────────────────────────────────

data class StatisticsUiState(
    // Navigation
    val timeFrame: TimeFrame = TimeFrame.DAY,
    val contentTab: ContentTab = ContentTab.EXPENSE,
    val anchorDate: LocalDate? = null,
    val headerLabel: String = "",

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
    val popularFoods: List<PopularFoodStat> = emptyList(),

    // Detail meal log list (CHI TIẾT section)
    val detailItems: List<DetailItem> = emptyList(),

    // Analysis tab
    val trendPoints: List<Float> = emptyList(),

    // Async
    val isLoading: Boolean = true,
    val error: String? = null
)