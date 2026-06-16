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
        // Bundle navigation params so flatMapLatest receives a single value
        NavParams(tf, tab, anchor, range, prevRange)
    }.flatMapLatest { nav ->
        // All inner flows react to the navigation params as a unit.
        combine(
            statisticsRepository.getTotalSpent(nav.range.start, nav.range.end),
            statisticsRepository.getItemCount(nav.range.start, nav.range.end),
            statisticsRepository.getTotalSpentForRange(nav.prevRange.start, nav.prevRange.end),
            statisticsRepository.getBarData(nav.timeFrame, nav.range.start, nav.range.end),
            statisticsRepository.getDonutData(nav.range.start, nav.range.end),
            statisticsRepository.getTopCategories(nav.range.start, nav.range.end),
            statisticsRepository.getWalletDestroyer(nav.range.start, nav.range.end),
            statisticsRepository.getPopularFoods(nav.range.start, nav.range.end)
        ) { args ->
            // combine with 8 flows gives an Array<Any?> — unpack by index
            @Suppress("UNCHECKED_CAST")
            val totalSpent      = args[0] as Double
            val itemCount       = args[1] as Int
            val previousTotal   = args[2] as Double
            val bars            = args[3] as List<ChartBar>
            val donut           = args[4] as List<ChartSlice>
            val topCats         = args[5] as List<CategoryStat>
            val destroyer       = args[6] as WalletDestroyerItem?
            val popular         = args[7] as List<PopularFoodStat>

            val summary = StatisticsSummary(
                totalSpent          = totalSpent,
                itemCount           = itemCount,
                previousPeriodTotal = previousTotal
            )

            // Budget state depends on the current totalSpent — flatMap again lazily.
            // We embed this as a suspended-but-inline calculation here, then
            // override with a dedicated combine below for live budget updates.
            // (Full live budget reactivity is wired in the outer combine below.)
            Triple(summary, bars, Pair(donut, Pair(topCats, Pair(destroyer, popular))))
        }.flatMapLatest { (summary, bars, rest) ->
            val (donut, rest2)     = rest
            val (topCats, rest3)   = rest2
            val (destroyer, popular) = rest3

            budgetFlow(nav.timeFrame, summary.totalSpent).map { budget ->
                // Trend points: last 4 periods' totals as Float for LocalLineTrendChartCard.
                // For now we have current + previous; extend to 4 points in Sprint 3.
                val trendPoints = buildTrendPoints(summary.previousPeriodTotal, summary.totalSpent)

                StatisticsUiState(
                    timeFrame    = nav.timeFrame,
                    contentTab   = nav.contentTab,
                    anchorDate   = nav.anchor,
                    headerLabel  = TimeRangeProvider.headerLabel(nav.timeFrame, nav.anchor),
                    summary      = summary,
                    budget       = budget,
                    barChartData = bars,
                    donutData    = donut,
                    topCategories    = topCats,
                    walletDestroyer  = destroyer,
                    popularFoods     = popular,
                    trendPoints      = trendPoints,
                    isLoading        = false,
                    error            = null
                )
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
     * Builds the Float list for [LocalLineTrendChartCard].
     *
     * Sprint 2: 2 points (previous → current).
     * Sprint 3 TODO: extend to 4+ points by pulling N-2 and N-3 period totals.
     */
    private fun buildTrendPoints(previous: Double, current: Double): List<Float> {
        // Ensure the line is always renderable — at least 2 non-zero points.
        val prev = previous.toFloat().coerceAtLeast(0f)
        val curr = current.toFloat().coerceAtLeast(0f)
        // Interpolate a midpoint for a smoother 3-point curve until Sprint 3 adds real data.
        val mid  = ((prev + curr) / 2f)
        return listOf(prev, mid, curr)
    }

    /** Bundle for combine → flatMapLatest handoff. */
    private data class NavParams(
        val timeFrame   : TimeFrame,
        val contentTab  : ContentTab,
        val anchor      : LocalDate,
        val range       : DateRange,
        val prevRange   : DateRange
    )
}