package com.SE114.food_tracker.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.util.TimeFrame
import com.SE114.food_tracker.core.util.TimeRangeProvider
import com.SE114.food_tracker.core.util.DateRange
import com.SE114.food_tracker.data.repository.BudgetRepository
import com.SE114.food_tracker.data.repository.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    // ── Navigation state ──────────────────────────────────────────────────────

    private val _timeFrame  = MutableStateFlow(TimeFrame.DAY)
    private val _contentTab = MutableStateFlow(ContentTab.EXPENSE)
    private val _anchor     = MutableStateFlow(TimeRangeProvider.today())

    // ── Derived range ─────────────────────────────────────────────────────────

    /** Half-open [start, end) epoch-millis range for the currently selected period. */
    private val _range: Flow<DateRange> =
        combine(_timeFrame, _anchor) { tf, anchor ->
            TimeRangeProvider.rangeFor(tf, anchor)
        }

    /** Previous period range — used for the "+X% vs last period" insight. */
    private val _previousRange: Flow<DateRange> =
        combine(_timeFrame, _anchor) { tf, anchor ->
            TimeRangeProvider.previousRangeFor(tf, anchor)
        }

    // ── Budget flow (from Room, scoped to current user) ───────────────────────

    /**
     * Emits a [BudgetUiState] whenever the budget row or the active time-frame changes.
     * When no user is authenticated yet (userId null), returns an empty BudgetUiState.
     */
    private fun budgetFlow(timeFrame: TimeFrame, spent: Double): Flow<BudgetUiState> {
        val userId = budgetRepository.getCurrentUserId() ?: return flowOf(BudgetUiState())
        return budgetRepository.getBudget(userId).map { budget ->
            val limit = when (timeFrame) {
                TimeFrame.DAY   -> budget?.daily
                TimeFrame.WEEK  -> budget?.weekly
                TimeFrame.MONTH -> budget?.monthly
                TimeFrame.YEAR  -> budget?.yearly
            }
            BudgetUiState(
                daily    = budget?.daily,
                weekly   = budget?.weekly,
                monthly  = budget?.monthly,
                yearly   = budget?.yearly,
                limit    = limit,
                spent    = spent
            )
        }
    }

    // ── Root UiState ──────────────────────────────────────────────────────────

    val uiState: StateFlow<StatisticsUiState> = combine(
        _timeFrame, _contentTab, _anchor, _range, _previousRange
    ) { tf, tab, anchor, range, prevRange ->
        NavParams(tf, tab, anchor, range, prevRange)
    }.flatMapLatest { nav ->
        val flow1 = statisticsRepository.getTotalSpent(nav.range.start, nav.range.end)
        val flow2 = statisticsRepository.getItemCount(nav.range.start, nav.range.end)
        val flow3 = statisticsRepository.getTotalSpentForRange(nav.prevRange.start, nav.prevRange.end)
        val flow4 = statisticsRepository.getBarData(nav.timeFrame, nav.range.start, nav.range.end)
        val flow5 = statisticsRepository.getDonutData(nav.range.start, nav.range.end)
        val flow6 = statisticsRepository.getTopCategories(nav.range.start, nav.range.end)
        val flow7 = statisticsRepository.getWalletDestroyer(nav.range.start, nav.range.end)

        combine(flow1, flow2, flow3, flow4, flow5, flow6, flow7) { args ->
            NavData(
                totalSpent    = args[0] as Double,
                itemCount     = args[1] as Int,
                previousTotal = args[2] as Double,
                bars          = args[3] as List<ChartBar>,
                donut         = args[4] as List<ChartSlice>,
                topCats       = args[5] as List<CategoryStat>,
                destroyer     = args[6] as WalletDestroyerItem?
            )
        }.flatMapLatest { nd ->
            combine(
                statisticsRepository.getDetailItems(nav.range.start, nav.range.end),
                statisticsRepository.getHistoricalCycleAverage(nav.timeFrame, nav.anchor)
            ) { detailItems, cycleAverage ->
                detailItems to cycleAverage
            }.flatMapLatest { (detailItems, cycleAverage) ->
                val summary = StatisticsSummary(
                    totalSpent          = nd.totalSpent,
                    itemCount           = nd.itemCount,
                    previousPeriodTotal = nd.previousTotal
                )
                budgetFlow(nav.timeFrame, nd.totalSpent).map { budget ->
                    val forecast = buildTrendForecast(
                        timeFrame      = nav.timeFrame,
                        anchor         = nav.anchor,
                        previousTotal  = nd.previousTotal,
                        currentActual  = nd.totalSpent,
                        cycleAverage   = cycleAverage
                    )
                    val datesWithData = detailItems
                        .map { it.entryDateEpoch }
                        .distinct()
                        .map { epochMs ->
                            kotlinx.datetime.Instant
                                .fromEpochMilliseconds(epochMs)
                                .toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
                                .dayOfMonth
                        }
                        .distinct()
                    StatisticsUiState(
                        timeFrame       = nav.timeFrame,
                        contentTab      = nav.contentTab,
                        anchorDate      = nav.anchor,
                        headerLabel     = TimeRangeProvider.headerLabel(nav.timeFrame, nav.anchor),
                        datesWithData   = datesWithData,
                        summary         = summary,
                        budget          = budget,
                        barChartData    = nd.bars,
                        donutData       = nd.donut,
                        topCategories   = nd.topCats,
                        walletDestroyer = nd.destroyer,
                        detailItems     = detailItems,
                        trendForecast   = forecast,
                        isLoading       = false,
                        error           = null
                    )
                }
            }
        }
    }
        .stateIn(
            scope          = viewModelScope,
            started        = SharingStarted.WhileSubscribed(5_000),
            initialValue   = StatisticsUiState(isLoading = true)
        )

    // ── User actions ──────────────────────────────────────────────────────────

    fun onTimeFrameSelected(tf: TimeFrame) {
        _timeFrame.value = tf
    }

    fun onContentTabSelected(tab: ContentTab) {
        _contentTab.value = tab
    }

    fun onPrevious() {
        _anchor.value = TimeRangeProvider.shift(_timeFrame.value, _anchor.value, forward = false)
    }

    fun onNext() {
        _anchor.value = TimeRangeProvider.shift(_timeFrame.value, _anchor.value, forward = true)
    }

    /** Called when user taps a day cell in CalendarCard. */
    fun onDateSelected(day: Int) {
        val current = _anchor.value
        _anchor.value = LocalDate(current.year, current.monthNumber, day)
    }

    /** Called when MonthYearPickerDialog confirms a new month/year. */
    fun onYearMonthSelected(month: Int, year: Int) {
        _anchor.value = LocalDate(year, month, 1)
    }

    /**
     * Save budget limits.
     *
     * Pass the complete set of 4 values as shown by the current dialog state.
     * [BudgetRepository.setBudget] merges with the existing row — null means "unchanged",
     * so pass null only if the user genuinely didn't touch that field.
     *
     * Sync is triggered by the existing [SyncScheduler] periodic worker; no immediate
     * sync is kicked here to match the offline-first pattern (PENDING → worker picks up).
     */
    fun saveBudget(
        daily: Double?,
        weekly: Double?,
        monthly: Double?,
        yearly: Double?
    ) {
        val userId = budgetRepository.getCurrentUserId()
        if (userId == null) {
            Timber.w("[StatisticsViewModel] saveBudget called with no authenticated user — skipping")
            return
        }
        viewModelScope.launch {
            runCatching {
                budgetRepository.setBudget(
                    userId  = userId,
                    daily   = daily,
                    weekly  = weekly,
                    monthly = monthly,
                    yearly  = yearly
                )
                Timber.d("[StatisticsViewModel] budget saved for $userId")
            }.onFailure { e ->
                Timber.e(e, "[StatisticsViewModel] failed to save budget")
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds the 3-point forecast for [LocalLineTrendChartCard] per Sprint 3 SRS #1/#2:
     *
     *   Projected Total = Current Period Spent + (7-cycle historical average * remaining cycles)
     *
     * "Remaining cycles" is the count of whole forecast-cycles left in the active period,
     * computed dynamically off the real system clock via [TimeRangeProvider.remainingCyclesInPeriod]
     * (cycle = day for WEEK/MONTH, week for YEAR). [TimeFrame.DAY] has no sub-cycle, so it
     * always has 0 remaining cycles and the projection collapses to the current actual spend.
     */
    private fun buildTrendForecast(
        timeFrame: TimeFrame,
        anchor: LocalDate,
        previousTotal: Double,
        currentActual: Double,
        cycleAverage: Double
    ): TrendForecast {
        val remaining = TimeRangeProvider.remainingCyclesInPeriod(timeFrame, anchor)
        val projected = currentActual + (cycleAverage.coerceAtLeast(0.0) * remaining)
        return TrendForecast(
            previousTotal    = previousTotal.coerceAtLeast(0.0),
            currentActual    = currentActual.coerceAtLeast(0.0),
            projectedTotal   = projected.coerceAtLeast(0.0),
            remainingCycles  = remaining
        )
    }

    /** Bundle for combine → flatMapLatest handoff. */
    private data class NavParams(
        val timeFrame   : TimeFrame,
        val contentTab  : ContentTab,
        val anchor      : LocalDate,
        val range       : DateRange,
        val prevRange   : DateRange
    )
    private data class NavData(
        val totalSpent    : Double,
        val itemCount     : Int,
        val previousTotal : Double,
        val bars          : List<ChartBar>,
        val donut         : List<ChartSlice>,
        val topCats       : List<CategoryStat>,
        val destroyer     : WalletDestroyerItem?
    )
}