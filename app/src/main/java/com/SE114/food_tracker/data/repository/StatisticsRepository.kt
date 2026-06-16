package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.core.util.TimeFrame
import com.SE114.food_tracker.core.util.toDayLabel
import com.SE114.food_tracker.core.util.toSessionLabel
import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.dao.ItemDAO
import com.SE114.food_tracker.data.local.dao.PopularFoodExpense
import com.SE114.food_tracker.data.local.entities.Item
import com.SE114.food_tracker.feature.stats.CategoryStat
import com.SE114.food_tracker.feature.stats.ChartBar
import com.SE114.food_tracker.feature.stats.ChartSlice
import com.SE114.food_tracker.feature.stats.PopularFoodStat
import com.SE114.food_tracker.feature.stats.WalletDestroyerItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Aggregates raw DAO flows into UI-ready chart models for the Statistics feature.
 *
 * Contract:
 * - All queries filter `wallet_id IS NULL` (enforced in ItemDAO) — personal stats only.
 * - Category metadata (name/icon) is joined from CategoryDAO — same pattern as
 *   [ItemRepository.getDiaryItemsByDay].
 * - No Vico types here — plain data classes keep this layer testable and TV4-agnostic.
 */
class StatisticsRepository @Inject constructor(
    private val itemDAO: ItemDAO,
    private val categoryDAO: CategoryDAO
) {

    // ── Summary ───────────────────────────────────────────────────────────────

    /**
     * Raw (total spend, item count) for a date range.
     * ViewModel combines this with previous-period total to build [StatisticsSummary].
     */
    fun getTotalSpent(start: Long, end: Long): Flow<Double> =
        itemDAO.getTotalExpenseForDay(start, end).map { it ?: 0.0 }

    fun getItemCount(start: Long, end: Long): Flow<Int> =
        itemDAO.getItemCountForDay(start, end)

    fun getTotalSpentForRange(start: Long, end: Long): Flow<Double> =
        itemDAO.getTotalExpenseForRange(start, end).map { it ?: 0.0 }

    // ── Bar chart ─────────────────────────────────────────────────────────────

    /**
     * Returns one [ChartBar] per time bucket:
     * - DAY   → 3 bars labelled "Sáng", "Trưa", "Tối" (time_type 0/1/2)
     * - WEEK  → up to 7 bars labelled "T2"…"CN"
     * - MONTH → up to 31 bars labelled "1"…"31"
     * - YEAR  → up to 12 bars labelled "Th1"…"Th12" (month-bucket query)
     *
     * Bars for missing days (zero spend) are omitted — TV4 pads them if needed.
     */
    fun getBarData(timeFrame: TimeFrame, start: Long, end: Long): Flow<List<ChartBar>> =
        when (timeFrame) {
            TimeFrame.DAY ->
                itemDAO.getExpenseByTimeType(start, end).map { rows ->
                    rows.map { ChartBar(label = it.timeType.toSessionLabel(), value = it.total) }
                }

            TimeFrame.WEEK, TimeFrame.MONTH ->
                itemDAO.getExpenseByDateBucket(start, end).map { rows ->
                    rows.map { ChartBar(label = it.entryDate.toDayLabel(timeFrame), value = it.total) }
                }

            TimeFrame.YEAR ->
                itemDAO.getExpenseByMonthBucket(start, end).map { rows ->
                    rows.map { ChartBar(label = it.monthEpoch.toDayLabel(timeFrame), value = it.total) }
                }
        }

    // ── Donut chart ───────────────────────────────────────────────────────────

    /**
     * Spend by category, decorated with category name and icon from Room.
     * Ordered by total DESC (matches DAO query).
     */
    fun getDonutData(start: Long, end: Long): Flow<List<ChartSlice>> =
        combine(
            itemDAO.getPersonalExpenseByCategory(start, end),
            categoryDAO.getAllCategories()
        ) { expenses, categories ->
            val byId = categories.associateBy { it.categoryId }
            expenses.map { exp ->
                val cat = byId[exp.categoryId]
                ChartSlice(
                    categoryId = exp.categoryId,
                    label      = cat?.name    ?: "Khác",
                    value      = exp.total,
                    iconUrl    = cat?.iconUrl ?: "🍽️"
                )
            }
        }

    // ── Insight cards ─────────────────────────────────────────────────────────

    /**
     * Top N categories by spend — derived from the donut data to avoid a duplicate query.
     */
    fun getTopCategories(start: Long, end: Long, limit: Int = 3): Flow<List<CategoryStat>> =
        getDonutData(start, end).map { slices ->
            slices.sortedByDescending { it.value }
                .take(limit)
                .map { CategoryStat(name = it.label, iconUrl = it.iconUrl, total = it.value) }
        }

    /**
     * The single most expensive personal item in the period ("Wallet Destroyer").
     * Returns null when there are no items.
     * Category metadata is joined so TV4 can render icon + category name without
     * touching CategoryDAO directly.
     */
    fun getWalletDestroyer(start: Long, end: Long): Flow<WalletDestroyerItem?> =
        combine(
            itemDAO.getTopExpensiveItems(start, end, limit = 1),
            categoryDAO.getAllCategories()
        ) { items, categories ->
            val item = items.firstOrNull() ?: return@combine null
            val byId = categories.associateBy { it.categoryId }
            val cat  = byId[item.categoryId]
            WalletDestroyerItem(
                itemId        = item.itemId,
                name          = item.name,
                categoryName  = cat?.name    ?: "Khác",
                categoryIconUrl = cat?.iconUrl ?: "🍽️",
                price         = item.price,
                currencyCode  = item.currencyCode,
                imageUrl      = item.imageUrl
            )
        }

    /**
     * Top N most frequently logged food names, tie-broken by total spend.
     */
    fun getPopularFoods(start: Long, end: Long, limit: Int = 5): Flow<List<PopularFoodStat>> =
        itemDAO.getPopularFoods(start, end, limit).map { rows ->
            rows.map { PopularFoodStat(name = it.name, recordCount = it.recordCount, totalSpent = it.totalSpent) }
        }
}