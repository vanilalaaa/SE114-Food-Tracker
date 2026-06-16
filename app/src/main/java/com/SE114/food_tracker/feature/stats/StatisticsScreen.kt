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

// Fixed donut color palette — defined at UI layer so the data layer stays color-agnostic.
private val DONUT_COLORS = listOf(
    Color(0xFFE8AEB4),
    Color(0xFFFBE3B5),
    Color(0xFFAED9E0),
    Color(0xFFD2EBD9),
    Color(0xFFD39292),
    Color(0xFFB5C4E3),
    Color(0xFFE8D3C7),
    Color(0xFFC9C0D3)
)

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

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
                            color      = Color(0xFF9B1C1C),
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
                        totalMeals    = uiState.budget.limit?.formatVnd() ?: "—",
                        totalSpending = uiState.summary.totalSpent.formatVnd(),
                        averagePerMeal = if (uiState.budget.hasLimit) {
                            val rem = uiState.budget.remaining
                            if (rem < 0) "-${(-rem).formatVnd()}" else rem.formatVnd()
                        } else "—",
                        label1 = "NGÂN SÁCH",
                        label2 = "ĐÃ CHI",
                        label3 = "CÒN LẠI"
                    )
                }

                // ── Bar chart: spend by session/day/month ────────────────────
                // Gap 2 fix: pass bar.value (Double) directly — ChartCard now
                // formats currency labels internally via its own formatVnd().
                if (uiState.barChartData.isNotEmpty()) {
                    ChartCard(
                        title = when (uiState.timeFrame) {
                            TimeFrame.DAY   -> "Chi tiêu theo buổi"
                            TimeFrame.WEEK  -> "Chi tiêu theo ngày"
                            TimeFrame.MONTH -> "Chi tiêu theo ngày"
                            TimeFrame.YEAR  -> "Chi tiêu theo tháng"
                        },
                        data = uiState.barChartData.map { bar ->
                            bar.label to bar.value   // Double — no lossy toInt()
                        }
                    )
                }

                // ── Donut chart: spend by category ───────────────────────────
                if (uiState.donutData.isNotEmpty()) {
                    LocalDonutChartCard(
                        title      = "Tỷ trọng chi tiêu theo danh mục",
                        categories = uiState.donutData.mapIndexed { i, slice ->
                            DonutSegment(
                                label = slice.label,
                                value = slice.value.toFloat(),
                                color = DONUT_COLORS[i % DONUT_COLORS.size]
                            )
                        }
                    )
                }

                // ── Popular foods ────────────────────────────────────────────
                if (uiState.popularFoods.isNotEmpty()) {
                    PopularFoodCard(
                        foodList = uiState.popularFoods.map { food ->
                            PopularFoodData(
                                name            = food.name,
                                recordCount     = food.recordCount.toString(),
                                imageUrl        = food.imageUrl,
                                categoryIconUrl = food.categoryIconUrl
                            )
                        }
                    )
                }

                // ── Detail meal list ─────────────────────────────────────────
                if (uiState.detailItems.isNotEmpty()) {
                    val isWeeklyMode = uiState.timeFrame != TimeFrame.DAY
                    DetailCardSection(
                        dataGroups   = uiState.detailItems.toDetailDayGroups(isWeeklyMode),
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

                if (uiState.trendPoints.size >= 2) {
                    LocalLineTrendChartCard(
                        title  = "Dự báo xu hướng kỳ tới",
                        points = uiState.trendPoints
                    )
                }

                Text(
                    text      = "CHỈ SỐ CHUYÊN SÂU",
                    style     = StatSectionTitleStyle,
                    color     = TextPrimaryStat,
                    fontSize  = 14.sp,
                    modifier  = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                InsightDashboardGrid(
                    topFood       = uiState.walletDestroyer?.name
                        ?: uiState.popularFoods.firstOrNull()?.name
                        ?: "—",
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

                val insightText = buildInsightText(uiState)
                InsightCard(insightText = insightText)
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
                        daily   = if (uiState.timeFrame == TimeFrame.DAY)   value else null,
                        weekly  = if (uiState.timeFrame == TimeFrame.WEEK)  value else null,
                        monthly = if (uiState.timeFrame == TimeFrame.MONTH) value else null,
                        yearly  = if (uiState.timeFrame == TimeFrame.YEAR)  value else null
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
    // Shown when the user taps the date label in StatisticsTopBar.
    // Uses a bare Dialog (no AlertDialog chrome) so CalendarCard fills naturally.
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

/** Format a Double as a short VND label, e.g. 120000.0 → "120K" */
private fun Double.formatVnd(): String {
    return when {
        this >= 1_000_000 -> "${"%.1f".format(this / 1_000_000)}M"
        this >= 1_000     -> "${(this / 1_000).toInt()}K"
        else              -> "${this.toInt()}"
    }
}

/** Convert tab index (0..3) → [TimeFrame] */
private fun Int.toTimeFrame(): TimeFrame = when (this) {
    0    -> TimeFrame.DAY
    1    -> TimeFrame.WEEK
    2    -> TimeFrame.MONTH
    else -> TimeFrame.YEAR
}

/** Convert [TimeFrame] → tab index for [TimeRangeSelector] */
private fun TimeFrame.toTabIndex(): Int = when (this) {
    TimeFrame.DAY   -> 0
    TimeFrame.WEEK  -> 1
    TimeFrame.MONTH -> 2
    TimeFrame.YEAR  -> 3
}

/**
 * Groups a flat [DetailItem] list into [DayGroup]s ready for [DetailCardSection].
 *
 * - DAY mode   : single group, no date header
 * - other modes: one group per unique [DetailItem.dateLabel], ordered by date DESC
 */
private fun List<DetailItem>.toDetailDayGroups(isWeeklyMode: Boolean): List<DayGroup> {
    if (isEmpty()) return emptyList()

    return if (!isWeeklyMode) {
        listOf(
            DayGroup(
                dateLabel = null,
                meals     = map { it.toMealRecord() }
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
                meals     = items.map { it.toMealRecord() }
            )
        }
    }
}

/**
 * Gap 1 fix: forward [DetailItem.categoryIconUrl] → [MealRecord.iconText]
 * and [DetailItem.imageUrl] → [MealRecord.imageUrl] so DetailItemRow can
 * render real thumbnails instead of the hardcoded peach placeholder.
 */
private fun DetailItem.toMealRecord(): MealRecord = MealRecord(
    time      = timeLabel,
    name      = name,
    category  = categoryName,
    price     = "${price.formatVnd()} đ",
    iconText  = categoryIconUrl,
    imageUrl  = imageUrl
)

/** Generate a simple insight sentence from the current UiState. */
private fun buildInsightText(state: StatisticsUiState): String {
    val destroyer = state.walletDestroyer
    val popular   = state.popularFoods.firstOrNull()

    return when {
        state.budget.isExceeded ->
            "Bạn đã vượt ngân sách ${(-state.budget.remaining).formatVnd()}đ — hãy điều chỉnh chi tiêu!"
        destroyer != null ->
            "\"${destroyer.name}\" là món tốn nhất kỳ này (${destroyer.price.formatVnd()}đ)"
        popular != null ->
            "\"${popular.name}\" là món được ghi nhận nhiều nhất (${popular.recordCount} lần)"
        state.summary.itemCount == 0 ->
            "Chưa có dữ liệu — hãy ghi nhật ký món ăn đầu tiên!"
        state.summary.isIncrease ->
            "Chi tiêu tăng ${"%.1f".format(state.summary.percentChange)}% so với kỳ trước"
        else ->
            "Chi tiêu giảm ${"%.1f".format(-state.summary.percentChange)}% so với kỳ trước 🎉"
    }
}