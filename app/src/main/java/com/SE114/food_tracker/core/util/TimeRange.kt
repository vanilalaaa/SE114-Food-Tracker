package com.SE114.food_tracker.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

// ─── Enums ───────────────────────────────────────────────────────────────────

enum class TimeFrame { DAY, WEEK, MONTH, YEAR }

// ─── Data holders ────────────────────────────────────────────────────────────

data class DateRange(val start: Long, val end: Long) // epoch millis, [start, end)

// ─── Provider ────────────────────────────────────────────────────────────────

/**
 * All date math is done in **UTC**, matching `Item.entryDate` which is stored as
 * UTC epoch-millis of midnight. Monday-start weeks are enforced via [weekStart].
 */
object TimeRangeProvider {

    private val tz = TimeZone.UTC

    // ── Public API ────────────────────────────────────────────────────────────

    fun today(): LocalDate = Clock.System.now().toLocalDateTime(tz).date

    /**
     * Returns the [start, end) half-open range covering the period that [anchor] belongs to
     * for the given [timeFrame].
     */
    fun rangeFor(timeFrame: TimeFrame, anchor: LocalDate): DateRange = when (timeFrame) {
        TimeFrame.DAY   -> dayRange(anchor)
        TimeFrame.WEEK  -> weekRange(anchor)
        TimeFrame.MONTH -> monthRange(anchor)
        TimeFrame.YEAR  -> yearRange(anchor)
    }

    /**
     * Returns the [start, end) range for the period **immediately before** [anchor]'s period.
     * Used for the "+X% vs last period" insight card.
     */
    fun previousRangeFor(timeFrame: TimeFrame, anchor: LocalDate): DateRange =
        rangeFor(timeFrame, shift(timeFrame, anchor, forward = false))

    /**
     * Shifts [anchor] one period forward or backward.
     * E.g. WEEK forward: Monday of the next week.
     */
    fun shift(timeFrame: TimeFrame, anchor: LocalDate, forward: Boolean): LocalDate {
        val delta = if (forward) 1 else -1
        return when (timeFrame) {
            TimeFrame.DAY   -> anchor.plus(DatePeriod(days = delta))
            TimeFrame.WEEK  -> anchor.plus(DatePeriod(days = delta * 7))
            TimeFrame.MONTH -> anchor.plus(DatePeriod(months = delta))
            TimeFrame.YEAR  -> anchor.plus(DatePeriod(years = delta))
        }
    }

    /**
     * Human-readable header label for [StatisticsTopBar].
     * Examples: "15/06/2026", "15 Th6 - 21 Th6", "Tháng 06/2026", "Năm 2026"
     */
    fun headerLabel(timeFrame: TimeFrame, anchor: LocalDate): String = when (timeFrame) {
        TimeFrame.DAY  -> "%02d/%02d/%04d".format(anchor.dayOfMonth, anchor.monthNumber, anchor.year)
        TimeFrame.WEEK -> {
            val start = weekStart(anchor)
            val end   = start.plus(DatePeriod(days = 6))
            "%d Th%d - %d Th%d".format(
                start.dayOfMonth, start.monthNumber,
                end.dayOfMonth,   end.monthNumber
            )
        }
        TimeFrame.MONTH -> "Tháng %02d/%04d".format(anchor.monthNumber, anchor.year)
        TimeFrame.YEAR  -> "Năm %04d".format(anchor.year)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun dayRange(anchor: LocalDate): DateRange {
        val start = anchor.atStartOfDayIn(tz).toEpochMilliseconds()
        val end   = anchor.plus(DatePeriod(days = 1)).atStartOfDayIn(tz).toEpochMilliseconds()
        return DateRange(start, end)
    }

    private fun weekRange(anchor: LocalDate): DateRange {
        val monday = weekStart(anchor)
        val start  = monday.atStartOfDayIn(tz).toEpochMilliseconds()
        val end    = monday.plus(DatePeriod(days = 7)).atStartOfDayIn(tz).toEpochMilliseconds()
        return DateRange(start, end)
    }

    private fun monthRange(anchor: LocalDate): DateRange {
        val first = LocalDate(anchor.year, anchor.monthNumber, 1)
        val start = first.atStartOfDayIn(tz).toEpochMilliseconds()
        val end   = first.plus(DatePeriod(months = 1)).atStartOfDayIn(tz).toEpochMilliseconds()
        return DateRange(start, end)
    }

    private fun yearRange(anchor: LocalDate): DateRange {
        val first = LocalDate(anchor.year, 1, 1)
        val start = first.atStartOfDayIn(tz).toEpochMilliseconds()
        val end   = LocalDate(anchor.year + 1, 1, 1).atStartOfDayIn(tz).toEpochMilliseconds()
        return DateRange(start, end)
    }

    /**
     * Returns the Monday of the ISO week that [date] belongs to.
     * kotlinx-datetime's [DayOfWeek.MONDAY] has ordinal 0 (Mon=0…Sun=6).
     */
    private fun weekStart(date: LocalDate): LocalDate {
        // ordinal: MONDAY=0, TUESDAY=1, … SUNDAY=6
        val daysFromMonday = date.dayOfWeek.ordinal // already 0-indexed from Mon
        return date.minus(DatePeriod(days = daysFromMonday))
    }
}

// ─── Extension helpers used by ViewModel ─────────────────────────────────────

/** Converts a time_type int (0/1/2) to a Vietnamese session label. */
fun Int.toSessionLabel(): String = when (this) {
    0    -> "Sáng"
    1    -> "Trưa"
    2    -> "Tối"
    else -> "Khác"
}

/**
 * Converts an epoch-millis [entry_date] to a short label appropriate for the [timeFrame].
 *  - WEEK  → "T2"…"CN" (Mon–Sun)
 *  - MONTH → "1"…"31"
 *  - YEAR  → "Th1"…"Th12"
 */
fun Long.toDayLabel(timeFrame: TimeFrame): String {
    val tz   = TimeZone.UTC
    val date = kotlinx.datetime.Instant.fromEpochMilliseconds(this).toLocalDateTime(tz).date
    return when (timeFrame) {
        TimeFrame.WEEK  -> when (date.dayOfWeek) {
            DayOfWeek.MONDAY    -> "T2"
            DayOfWeek.TUESDAY   -> "T3"
            DayOfWeek.WEDNESDAY -> "T4"
            DayOfWeek.THURSDAY  -> "T5"
            DayOfWeek.FRIDAY    -> "T6"
            DayOfWeek.SATURDAY  -> "T7"
            DayOfWeek.SUNDAY    -> "CN"
            else                -> "?"
        }
        TimeFrame.MONTH -> date.dayOfMonth.toString()
        TimeFrame.YEAR  -> "Th${date.monthNumber}"
        TimeFrame.DAY   -> "%02d:%02d".format(date.dayOfMonth, date.monthNumber) // fallback
    }
}