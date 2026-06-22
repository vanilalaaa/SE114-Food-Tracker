package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.core.network.SessionProvider
import com.SE114.food_tracker.core.util.TimeFrame
import com.SE114.food_tracker.core.util.TimeRangeProvider
import com.SE114.food_tracker.core.util.toDayLabel
import com.SE114.food_tracker.core.util.toSessionLabel
import com.SE114.food_tracker.data.local.dao.CategoryDAO
import com.SE114.food_tracker.data.local.dao.ItemDAO
import com.SE114.food_tracker.feature.stats.CategoryStat
import com.SE114.food_tracker.feature.stats.ChartBar
import com.SE114.food_tracker.feature.stats.ChartSlice
import com.SE114.food_tracker.feature.stats.DetailItem
import com.SE114.food_tracker.feature.stats.WalletDestroyerItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

class StatisticsRepository @Inject constructor(
    private val itemDAO: ItemDAO,
    private val categoryDAO: CategoryDAO,
    private val sessionProvider: SessionProvider
) {

    private fun owner(): String = sessionProvider.currentUserId().orEmpty()

    // ── Summary ───────────────────────────────────────────────────────────────

    fun getTotalSpent(start: Long, end: Long): Flow<Double> =
        itemDAO.getTotalExpenseForDay(owner(), start, end).map { it ?: 0.0 }

    fun getItemCount(start: Long, end: Long): Flow<Int> =
        itemDAO.getItemCountForDay(owner(), start, end)

    fun getTotalSpentForRange(start: Long, end: Long): Flow<Double> =
        itemDAO.getTotalExpenseForRange(owner(), start, end).map { it ?: 0.0 }

    /**
     * Average personal spend per forecast-cycle over the last 7 cycles immediately
     * preceding the active period (Sprint 3 SRS #1). Cycle granularity and the exact
     * lookback window are computed by [TimeRangeProvider.lastSevenCyclesRangeFor] —
     * WEEK/MONTH look back 7 days, YEAR looks back 7 weeks.
     *
     * `wallet_id IS NULL` is already enforced inside [ItemDAO.getTotalExpenseForRange],
     * so shared-wallet expenses never leak into this average (SRS #3).
     */
    fun getHistoricalCycleAverage(timeFrame: TimeFrame, anchor: LocalDate): Flow<Double> {
        val lookback = TimeRangeProvider.lastSevenCyclesRangeFor(timeFrame, anchor)
        return itemDAO.getTotalExpenseForRange(owner(), lookback.start, lookback.end).map { total ->
            (total ?: 0.0) / 7.0
        }
    }

    // ── Bar chart ─────────────────────────────────────────────────────────────

    fun getBarData(timeFrame: TimeFrame, start: Long, end: Long): Flow<List<ChartBar>> =
        when (timeFrame) {

            // DAY — always 3 bars: Sáng / Trưa / Tối
            TimeFrame.DAY ->
                itemDAO.getExpenseByTimeType(owner(), start, end).map { rows ->
                    val byType = rows.associateBy { it.timeType }
                    listOf(0, 1, 2).map { t ->
                        ChartBar(label = t.toSessionLabel(), value = byType[t]?.total ?: 0.0)
                    }
                }

            // WEEK — always 7 bars: T2 … CN, in Mon→Sun order
            TimeFrame.WEEK ->
                itemDAO.getExpenseByDateBucket(owner(), start, end).map { rows ->
                    val byLabel = rows.associate { it.entryDate.toDayLabel(TimeFrame.WEEK) to it.total }
                    listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").map { label ->
                        ChartBar(label = label, value = byLabel[label] ?: 0.0)
                    }
                }

            // MONTH — 4 bars representing weeks 1–4 of the month.
            // Week boundaries: [1–7], [8–14], [15–21], [22–28/29/30/31]
            TimeFrame.MONTH ->
                itemDAO.getExpenseByDateBucket(owner(), start, end).map { rows ->
                    // Group all item dates into 4 week buckets and sum by bucket
                    val weekBuckets = mutableMapOf(1 to 0.0, 2 to 0.0, 3 to 0.0, 4 to 0.0)
                    rows.forEach { expense ->
                        val expenseDate = Instant.fromEpochMilliseconds(expense.entryDate)
                            .toLocalDateTime(TimeZone.UTC).date
                        val dayOfMonth = expenseDate.dayOfMonth
                        val week = when {
                            dayOfMonth <= 7  -> 1
                            dayOfMonth <= 14 -> 2
                            dayOfMonth <= 21 -> 3
                            else             -> 4
                        }
                        weekBuckets[week] = weekBuckets[week]!! + expense.total
                    }

                    (1..4).map { week ->
                        ChartBar(label = "Tuần $week", value = weekBuckets[week] ?: 0.0)
                    }
                }

            // YEAR — always 12 bars: Th1 … Th12
            TimeFrame.YEAR ->
                itemDAO.getExpenseByMonthBucket(owner(), start, end).map { rows ->
                    val byLabel = rows.associate { it.monthEpoch.toDayLabel(TimeFrame.YEAR) to it.total }
                    (1..12).map { m ->
                        ChartBar(label = "T$m", value = byLabel["T$m"] ?: 0.0)
                    }
                }
        }

    // ── Donut chart ───────────────────────────────────────────────────────────

    fun getDonutData(start: Long, end: Long): Flow<List<ChartSlice>> =
        combine(
            itemDAO.getPersonalExpenseByCategory(owner(), start, end),
            categoryDAO.getAllCategories(owner())
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

    fun getTopCategories(start: Long, end: Long, limit: Int = 3): Flow<List<CategoryStat>> =
        getDonutData(start, end).map { slices ->
            slices.sortedByDescending { it.value }
                .take(limit)
                .map { CategoryStat(name = it.label, iconUrl = it.iconUrl, total = it.value) }
        }

    fun getWalletDestroyer(start: Long, end: Long): Flow<WalletDestroyerItem?> =
        combine(
            itemDAO.getTopExpensiveItems(owner(), start, end, limit = 1),
            categoryDAO.getAllCategories(owner())
        ) { items, categories ->
            val item = items.firstOrNull() ?: return@combine null
            val byId = categories.associateBy { it.categoryId }
            val cat  = byId[item.categoryId]
            WalletDestroyerItem(
                itemId          = item.itemId,
                name            = item.name,
                categoryName    = cat?.name    ?: "Khác",
                categoryIconUrl = cat?.iconUrl ?: "🍽️",
                price           = item.price,
                currencyCode    = item.currencyCode,
                imageUrl        = item.imageUrl
            )
        }

    // ── Detail list ───────────────────────────────────────────────────────────

    /**
     * All personal items in [start, end), enriched with category metadata,
     * ready for [DetailCardSection].
     * Ordered by entry_date DESC then time_type ASC (ItemDAO.getItemsByDateRange).
     */
    fun getDetailItems(start: Long, end: Long): Flow<List<DetailItem>> =
        combine(
            itemDAO.getItemsByDateRange(owner(), start, end),
            categoryDAO.getAllCategories(owner())
        ) { items, categories ->
            val byId = categories.associateBy { it.categoryId }
            val tz   = TimeZone.UTC

            items.map { item ->
                val cat  = byId[item.categoryId]
                val date = Instant.fromEpochMilliseconds(item.entryDate)
                    .toLocalDateTime(tz).date

                val dayName = when (date.dayOfWeek) {
                    DayOfWeek.MONDAY    -> "Thứ Hai"
                    DayOfWeek.TUESDAY   -> "Thứ Ba"
                    DayOfWeek.WEDNESDAY -> "Thứ Tư"
                    DayOfWeek.THURSDAY  -> "Thứ Năm"
                    DayOfWeek.FRIDAY    -> "Thứ Sáu"
                    DayOfWeek.SATURDAY  -> "Thứ Bảy"
                    DayOfWeek.SUNDAY    -> "Chủ Nhật"
                    else                -> ""
                }

                DetailItem(
                    itemId          = item.itemId,
                    name            = item.name,
                    categoryName    = cat?.name     ?: "Khác",
                    categoryIconUrl = cat?.iconUrl  ?: "🍽️",
                    price           = item.price,
                    currencyCode    = item.currencyCode,
                    timeLabel       = item.timeType.toSessionLabel(),
                    dateLabel       = "$dayName, %02d/%02d".format(date.dayOfMonth, date.monthNumber),
                    entryDateEpoch  = item.entryDate,
                    imageUrl        = item.imageUrl
                )
            }
        }
}