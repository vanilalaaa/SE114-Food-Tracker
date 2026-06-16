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
import com.SE114.food_tracker.feature.stats.DetailItem
import com.SE114.food_tracker.feature.stats.PopularFoodStat
import com.SE114.food_tracker.feature.stats.WalletDestroyerItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

class StatisticsRepository @Inject constructor(
    private val itemDAO: ItemDAO,
    private val categoryDAO: CategoryDAO
) {

    // ── Summary ───────────────────────────────────────────────────────────────

    fun getTotalSpent(start: Long, end: Long): Flow<Double> =
        itemDAO.getTotalExpenseForDay(start, end).map { it ?: 0.0 }

    fun getItemCount(start: Long, end: Long): Flow<Int> =
        itemDAO.getItemCountForDay(start, end)

    fun getTotalSpentForRange(start: Long, end: Long): Flow<Double> =
        itemDAO.getTotalExpenseForRange(start, end).map { it ?: 0.0 }

    // ── Bar chart ─────────────────────────────────────────────────────────────

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

    fun getTopCategories(start: Long, end: Long, limit: Int = 3): Flow<List<CategoryStat>> =
        getDonutData(start, end).map { slices ->
            slices.sortedByDescending { it.value }
                .take(limit)
                .map { CategoryStat(name = it.label, iconUrl = it.iconUrl, total = it.value) }
        }

    fun getWalletDestroyer(start: Long, end: Long): Flow<WalletDestroyerItem?> =
        combine(
            itemDAO.getTopExpensiveItems(start, end, limit = 1),
            categoryDAO.getAllCategories()
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

    fun getPopularFoods(start: Long, end: Long, limit: Int = 5): Flow<List<PopularFoodStat>> =
        combine(
            itemDAO.getPopularFoods(start, end, limit),
            itemDAO.getItemsByDateRange(start, end),
            categoryDAO.getAllCategories()
        ) { rows, allItems, categories ->
            val byId = categories.associateBy { it.categoryId }
            // Index items by name → pick the most recent one (highest updatedAt) per name
            val latestItemByName: Map<String, com.SE114.food_tracker.data.local.entities.Item> =
                allItems.groupBy { it.name }
                    .mapValues { (_, items) -> items.maxBy { it.updatedAt } }
            rows.map { row ->
                val item = latestItemByName[row.name]
                val cat  = item?.categoryId?.let { byId[it] }
                PopularFoodStat(
                    name            = row.name,
                    recordCount     = row.recordCount,
                    totalSpent      = row.totalSpent,
                    imageUrl        = item?.imageUrl,
                    categoryIconUrl = cat?.iconUrl ?: "🍽️"
                )
            }
        }

    // ── Detail list ───────────────────────────────────────────────────────────

    /**
     * All personal items in [start, end), enriched with category metadata,
     * ready for [DetailCardSection].
     * Ordered by entry_date DESC then time_type ASC (ItemDAO.getItemsByDateRange).
     */
    fun getDetailItems(start: Long, end: Long): Flow<List<DetailItem>> =
        combine(
            itemDAO.getItemsByDateRange(start, end),
            categoryDAO.getAllCategories()
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