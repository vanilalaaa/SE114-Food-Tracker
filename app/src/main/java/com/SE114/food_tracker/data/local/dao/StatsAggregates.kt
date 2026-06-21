package com.SE114.food_tracker.data.local.dao

import androidx.room.ColumnInfo

/**
 * DAO result shapes for statistics queries.
 * These are SQLite projection classes — they must only use @ColumnInfo names that
 * exactly match the column aliases produced by the SQL GROUP BY queries in ItemDAO.
 *
 * Keep alongside CategoryExpense.kt in the dao package.
 */

/** Bar chart: spend grouped by time_type (0=Sáng, 1=Trưa, 2=Tối) — used for DAY view */
data class TimeTypeExpense(
    @ColumnInfo(name = "time_type") val timeType: Int,
    val total: Double,
    val count: Int
)

/** Bar chart: spend grouped by entry_date epoch (one row per day) — used for WEEK/MONTH view */
data class DateExpense(
    @ColumnInfo(name = "entry_date") val entryDate: Long,
    val total: Double
)

/**
 * Bar chart: spend grouped by calendar month — used for YEAR view (12 bars max).
 *
 * SQLite doesn't have a native month-extraction function, so the query uses integer
 * arithmetic on the UTC epoch-millis column. See [ItemDAO.getExpenseByMonthBucket].
 *
 * [monthEpoch] is the epoch-millis of the 1st of that month at UTC midnight,
 * computed in the DAO query for easy labelling.
 */
data class MonthExpense(
    @ColumnInfo(name = "month_epoch") val monthEpoch: Long,
    val total: Double
)

/** "Phổ biến nhất" card: most frequently logged food names */
data class PopularFoodExpense(
    val name: String,
    val recordCount: Int,
    val totalSpent: Double
)