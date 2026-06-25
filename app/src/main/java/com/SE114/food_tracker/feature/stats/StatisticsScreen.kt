package com.SE114.food_tracker.feature.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SE114.food_tracker.core.designsystem.components.*
import com.SE114.food_tracker.core.util.TimeFrame
import com.SE114.food_tracker.feature.stats.components.*
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.core.util.CurrencyDisplay
import com.SE114.food_tracker.core.util.LocalCurrencyDisplay

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val currency = LocalCurrencyDisplay.current

    // Budget dialog state (purely local UI)
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf("") }
    var showCalendarDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            StatisticsTopBar(
                dateText = uiState.headerLabel,
                onDateClick = {
                    // TopBar date area → always opens the CalendarCard picker
                    showCalendarDialog = true
                },
                onNextClick     = { viewModel.onNext() },
                onPreviousClick = { viewModel.onPrevious() }
            )
        },
        containerColor = MainBackground
    ) { innerPadding ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StatPinkDark)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Time-frame selector ──────────────────────────────────────────
            TimeRangeSelector(
                selectedTab   = uiState.timeFrame.toTabIndex(),
                onTabSelected = { viewModel.onTimeFrameSelected(it.toTimeFrame()) }
            )

            // ── Content tab (Chi tiêu / Phân tích) ──────────────────────────
            TabRow(
                selectedTabIndex = uiState.contentTab.ordinal,
                containerColor   = MainBackground,
                contentColor     = StatPinkDark
            ) {
                Tab(
                    selected = uiState.contentTab == ContentTab.EXPENSE,
                    onClick  = { viewModel.onContentTabSelected(ContentTab.EXPENSE) },
                    text     = { Text("Chi tiêu", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.contentTab == ContentTab.ANALYSIS,
                    onClick  = { viewModel.onContentTabSelected(ContentTab.ANALYSIS) },
                    text     = { Text("Phân tích", fontWeight = FontWeight.Bold) }
                )
            }

            // ── Budget exceeded banner ───────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.budget.isExceeded,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFFFDF2F2)),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier          = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text       = "Chi tiêu hiện tại đã vượt quá ngân sách kỳ này!",
                            color      = TextWarning,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ════════════════════════════════════════════════════════════════
            // CHI TIÊU tab
            // ════════════════════════════════════════════════════════════════
            if (uiState.contentTab == ContentTab.EXPENSE) {

                // ── Budget summary grid (tap to open budget dialog) ──────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val current = when (uiState.timeFrame) {
                                TimeFrame.DAY   -> uiState.budget.daily
                                TimeFrame.WEEK  -> uiState.budget.weekly
                                TimeFrame.MONTH -> uiState.budget.monthly
                                TimeFrame.YEAR  -> uiState.budget.yearly
                            }
                            budgetInput = current?.toInt()?.toString() ?: ""
                            showBudgetDialog = true
                        }
                ) {
                    StatisticsSummaryGrid(
                        totalMeals    = uiState.budget.limit?.let { currency.formatShort(it) } ?: "—",
                        totalSpending = currency.formatShort(uiState.summary.totalSpent),
                        averagePerMeal = if (uiState.budget.hasLimit) {
                            val rem = uiState.budget.remaining
                            if (rem < 0) "-${currency.formatShort(-rem)}" else currency.formatShort(rem)
                        } else "—",
                        label1 = "NGÂN SÁCH",
                        label2 = "ĐÃ CHI",
                        label3 = "CÒN LẠI"
                    )
                }

                // ── Bar chart: spend by session/day/month ────────────────────
                if (uiState.barChartData.isNotEmpty()) {
                    ChartCard(
                        title = when (uiState.timeFrame) {
                            TimeFrame.DAY   -> "Chi tiêu theo buổi"
                            TimeFrame.WEEK  -> "Chi tiêu theo ngày"
                            TimeFrame.MONTH -> "Chi tiêu theo ngày"
                            TimeFrame.YEAR  -> "Chi tiêu theo tháng"
                        },
                        data = uiState.barChartData.map { bar ->
                            bar.label to bar.value
                        }
                    )
                }

                // ── Donut chart: spend by category ───────────────────────────
                if (uiState.donutData.isNotEmpty()) {
                    val donutColors = listOf(
                        DonutSegment1,
                        DonutSegment2,
                        DonutSegment3,
                        DonutSegment4,
                        DonutSegment5,
                        DonutSegment6,
                        DonutSegment7,
                        DonutSegment8
                    )
                    LocalDonutChartCard(
                        title      = "Tỷ trọng chi tiêu theo danh mục",
                        categories = uiState.donutData.mapIndexed { i, slice ->
                            DonutSegment(
                                label = slice.label,
                                value = slice.value.toFloat(),
                                color = donutColors[i % donutColors.size]
                            )
                        }
                    )
                }

                // ── Detail meal list ─────────────────────────────────────────
                if (uiState.detailItems.isNotEmpty()) {
                    val isWeeklyMode = uiState.timeFrame != TimeFrame.DAY
                    DetailCardSection(
                        dataGroups   = uiState.detailItems.toDetailDayGroups(isWeeklyMode, currency),
                        isWeeklyMode = isWeeklyMode
                    )
                } else {
                    Box(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment  = Alignment.Center
                    ) {
                        Text(
                            text      = "Chưa có món ăn nào trong kỳ này",
                            color     = TextLabelGray,
                            fontSize  = 14.sp
                        )
                    }
                }

            } else {
                // ════════════════════════════════════════════════════════════
                // PHÂN TÍCH tab
                // ════════════════════════════════════════════════════════════

                ForecastCard(
                    forecast    = uiState.trendForecast,
                    budgetLimit = uiState.budget.limit,
                    timeFrame   = uiState.timeFrame,
                    anchorDate  = uiState.anchorDate ?: com.SE114.food_tracker.core.util.TimeRangeProvider.today()
                )

                // ── Wallet Destroyer ──────────────────────────────────────────
                WalletDestroyerCard(item = uiState.walletDestroyer)

                // ── Top categories by spend ───────────────────────────────────
                if (uiState.topCategories.isNotEmpty()) {
                    TopCategoriesCard(categories = uiState.topCategories)
                }

                Text(
                    text      = "CHỈ SỐ CHUYÊN SÂU",
                    style     = StatSectionTitleStyle,
                    color     = TextPrimaryStat,
                    fontSize  = 14.sp,
                    modifier  = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                InsightDashboardGrid(
                    topFood       = uiState.walletDestroyer?.name ?: "—",
                    topCategory   = uiState.topCategories.firstOrNull()?.let {
                        "${it.iconUrl} ${it.name}"
                    } ?: "—",
                    variationText = uiState.summary.let { s ->
                        if (s.previousPeriodTotal == 0.0) "—"
                        else {
                            val pct = s.percentChange
                            val sign = if (pct >= 0) "+" else ""
                            val trend = if (s.isIncrease) "Tăng" else "Giảm"
                            "$sign${"%.1f".format(pct)}% ($trend)"
                        }
                    }
                )

                val dynamicInsights = uiState.getDynamicInsights(currency.displayCurrency, currency.rates)
                val finalInsights = dynamicInsights.ifEmpty { listOf(buildInsightText(uiState, currency)) }

                InsightCard(insights = finalInsights)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ── Budget dialog ────────────────────────────────────────────────────────
    if (showBudgetDialog) {
        val periodLabel = when (uiState.timeFrame) {
            TimeFrame.DAY   -> "Ngày"
            TimeFrame.WEEK  -> "Tuần"
            TimeFrame.MONTH -> "Tháng"
            TimeFrame.YEAR  -> "Năm"
        }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = {
                Text("Đặt ngân sách $periodLabel", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hạn mức mong muốn (đ):")
                    OutlinedTextField(
                        value         = budgetInput,
                        onValueChange = { budgetInput = it },
                        label         = { Text("Số tiền") },
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val value = budgetInput.toDoubleOrNull()
                    viewModel.saveBudget(
                        daily   = if (uiState.timeFrame == TimeFrame.DAY)   value else uiState.budget.daily,
                        weekly  = if (uiState.timeFrame == TimeFrame.WEEK)  value else uiState.budget.weekly,
                        monthly = if (uiState.timeFrame == TimeFrame.MONTH) value else uiState.budget.monthly,
                        yearly  = if (uiState.timeFrame == TimeFrame.YEAR)  value else uiState.budget.yearly
                    )
                    showBudgetDialog = false
                }) { Text("Lưu lại") }
            },
            dismissButton = {
                Button(onClick = { showBudgetDialog = false }) { Text("Hủy") }
            }
        )
    }

    // ── Calendar picker dialog ───────────────────────────────────────────────
    if (showCalendarDialog) {
        val anchor = uiState.anchorDate
        if (anchor != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showCalendarDialog = false }
            ) {
                DayPickerDialog(
                    selectedYear       = anchor.year,
                    selectedMonth      = anchor.monthNumber,
                    onDateClick        = { day ->
                        viewModel.onDateSelected(day)
                        showCalendarDialog = false
                    },
                    onMonthYearChanged = { month, year ->
                        viewModel.onYearMonthSelected(month, year)
                    },
                    hasDataDates       = uiState.datesWithData
                )
            }
        }
    }
}

// ─── Mappers & helpers ────────────────────────────────────────────────────────

private fun Int.toTimeFrame(): TimeFrame = when (this) {
    0    -> TimeFrame.DAY
    1    -> TimeFrame.WEEK
    2    -> TimeFrame.MONTH
    else -> TimeFrame.YEAR
}

private fun TimeFrame.toTabIndex(): Int = when (this) {
    TimeFrame.DAY   -> 0
    TimeFrame.WEEK  -> 1
    TimeFrame.MONTH -> 2
    TimeFrame.YEAR  -> 3
}

private fun List<DetailItem>.toDetailDayGroups(isWeeklyMode: Boolean, currency: CurrencyDisplay): List<DayGroup> {
    if (isEmpty()) return emptyList()

    return if (!isWeeklyMode) {
        listOf(
            DayGroup(
                dateLabel = null,
                meals     = map { it.toMealRecord(currency) }
            )
        )
    } else {
        val ordered = LinkedHashMap<String, MutableList<DetailItem>>()
        forEach { item ->
            ordered.getOrPut(item.dateLabel) { mutableListOf() }.add(item)
        }
        ordered.map { (label, items) ->
            DayGroup(
                dateLabel = label,
                meals     = items.map { it.toMealRecord(currency) }
            )
        }
    }
}

private fun DetailItem.toMealRecord(currency: CurrencyDisplay): MealRecord = MealRecord(
    time      = timeLabel,
    name      = name,
    category  = categoryName,
    price     = currency.format(price, currencyCode),
    iconText  = categoryIconUrl,
    imageUrl  = imageUrl
)

private fun buildInsightText(state: StatisticsUiState, currency: CurrencyDisplay): String {
    val destroyer = state.walletDestroyer
    val topCat    = state.topCategories.firstOrNull()

    return when {
        state.budget.isExceeded ->
            "Bạn đã vượt ngân sách ${currency.formatShort(-state.budget.remaining)} — hãy điều chỉnh chi tiêu!"
        destroyer != null ->
            "\"${destroyer.name}\" là món tốn nhất kỳ này (${currency.formatShort(destroyer.price)})"
        topCat != null ->
            "Danh mục tốn nhiều nhất: ${topCat.iconUrl} ${topCat.name} (${currency.formatShort(topCat.total)})"
        state.summary.itemCount == 0 ->
            "Chưa có dữ liệu — hãy ghi nhật ký món ăn đầu tiên!"
        state.summary.isIncrease ->
            "Chi tiêu tăng ${"%.1f".format(state.summary.percentChange)}% so với kỳ trước"
        else ->
            "Chi tiêu giảm ${"%.1f".format(-state.summary.percentChange)}% so với kỳ trước 🎉"
    }
}