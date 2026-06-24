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
    private val _range: Flow<DateRange> =
        combine(_timeFrame, _anchor) { tf, anchor ->
            TimeRangeProvider.rangeFor(tf, anchor)
        }

    private val _previousRange: Flow<DateRange> =
        combine(_timeFrame, _anchor) { tf, anchor ->
            TimeRangeProvider.previousRangeFor(tf, anchor)
        }

    // ── Budget flow ───────────────────────────────────────────────────────────
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
            statisticsRepository.getDetailItems(nav.range.start, nav.range.end).flatMapLatest { detailItems ->
                val summary = StatisticsSummary(
                    totalSpent          = nd.totalSpent,
                    itemCount           = nd.itemCount,
                    previousPeriodTotal = nd.previousTotal
                )
                budgetFlow(nav.timeFrame, nd.totalSpent).map { budget ->
                    val forecast = TrendForecast(
                        previousTotal   = nd.previousTotal.coerceAtLeast(0.0),
                        currentActual   = nd.totalSpent.coerceAtLeast(0.0),
                        projectedTotal  = 0.0, // UI tự tính toán dựa trên Live Rate
                        remainingCycles = 0
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
    fun onTimeFrameSelected(tf: TimeFrame) { _timeFrame.value = tf }
    fun onContentTabSelected(tab: ContentTab) { _contentTab.value = tab }
    fun onPrevious() { _anchor.value = TimeRangeProvider.shift(_timeFrame.value, _anchor.value, forward = false) }
    fun onNext() { _anchor.value = TimeRangeProvider.shift(_timeFrame.value, _anchor.value, forward = true) }

    fun onDateSelected(day: Int) {
        val current = _anchor.value
        _anchor.value = LocalDate(current.year, current.monthNumber, day)
    }

    fun onYearMonthSelected(month: Int, year: Int) {
        _anchor.value = LocalDate(year, month, 1)
    }

    fun saveBudget(daily: Double?, weekly: Double?, monthly: Double?, yearly: Double?) {
        val userId = budgetRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            runCatching {
                budgetRepository.setBudget(
                    userId = userId, daily = daily, weekly = weekly, monthly = monthly, yearly = yearly
                )
            }.onFailure { e ->
                Timber.e(e, "[StatisticsViewModel] failed to save budget")
            }
        }
    }

    private data class NavParams(
        val timeFrame: TimeFrame, val contentTab: ContentTab, val anchor: LocalDate, val range: DateRange, val prevRange: DateRange
    )
    private data class NavData(
        val totalSpent: Double, val itemCount: Int, val previousTotal: Double, val bars: List<ChartBar>, val donut: List<ChartSlice>, val topCats: List<CategoryStat>, val destroyer: WalletDestroyerItem?
    )
}