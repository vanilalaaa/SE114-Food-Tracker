package com.SE114.food_tracker.feature.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.components.AppButton
import com.SE114.food_tracker.core.designsystem.components.AppButtonVariant
import com.SE114.food_tracker.core.designsystem.components.AppScaffold
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.data.repository.AdminReport
import com.SE114.food_tracker.feature.admin.components.AdminReportRow
import com.SE114.food_tracker.feature.admin.components.AdminTopBar
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun AdminReportsScreen(
    onBack: () -> Unit,
    viewModel: AdminReportsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedReport by remember { mutableStateOf<AdminReport?>(null) }

    val actionErrorText = state.actionError?.adminMessage()
    LaunchedEffect(state.actionError) {
        if (actionErrorText != null) {
            snackbarHostState.showSnackbar(actionErrorText)
            viewModel.clearActionError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AdminReportsContent(
            state = state,
            onBack = onBack,
            onFilterChange = viewModel::onFilterChange,
            onRetry = viewModel::reload,
            onLoadMore = viewModel::loadMore,
            onReportClick = { selectedReport = it }
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }

    selectedReport?.let { report ->
        ReportActionSheet(
            report = report,
            onDismiss = { selectedReport = null },
            onResolve = { status, ban ->
                selectedReport = null
                viewModel.resolve(report, status, ban)
            }
        )
    }
}

@Composable
private fun AdminReportsContent(
    state: AdminReportsUiState,
    onBack: () -> Unit,
    onFilterChange: (ReportStatusFilter) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onReportClick: (AdminReport) -> Unit
) {
    AppScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            AdminTopBar(title = stringResource(R.string.admin_reports_title), onBack = onBack)
            Spacer(Modifier.height(8.dp))
            FilterRow(selected = state.filter, onFilterChange = onFilterChange)
            Spacer(Modifier.height(8.dp))

            val listState = rememberLazyListState()
            LaunchedEffect(listState, state.reports.size, state.canLoadMore) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
                    .distinctUntilChanged()
                    .collect { lastVisible ->
                        if (state.canLoadMore && lastVisible >= state.reports.lastIndex - 3) onLoadMore()
                    }
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    state.isLoading && state.reports.isEmpty() ->
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    state.error != null && state.reports.isEmpty() -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(state.error.adminMessage(), color = MaterialTheme.colorScheme.error)
                        AppButton(
                            text = stringResource(R.string.admin_retry),
                            onClick = onRetry,
                            variant = AppButtonVariant.Secondary
                        )
                    }

                    state.reports.isEmpty() -> Text(
                        text = stringResource(R.string.admin_reports_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.reports, key = { it.id }) { report ->
                            AdminReportRow(report = report, onClick = { onReportClick(report) })
                        }
                        if (state.isLoadingMore) {
                            item {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    selected: ReportStatusFilter,
    onFilterChange: (ReportStatusFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChipItem(
            label = stringResource(R.string.admin_reports_filter_pending),
            selected = selected == ReportStatusFilter.PENDING,
            onClick = { onFilterChange(ReportStatusFilter.PENDING) }
        )
        FilterChipItem(
            label = stringResource(R.string.admin_reports_filter_resolved),
            selected = selected == ReportStatusFilter.RESOLVED,
            onClick = { onFilterChange(ReportStatusFilter.RESOLVED) }
        )
        FilterChipItem(
            label = stringResource(R.string.admin_reports_filter_dismissed),
            selected = selected == ReportStatusFilter.DISMISSED,
            onClick = { onFilterChange(ReportStatusFilter.DISMISSED) }
        )
    }
}

@Composable
private fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportActionSheet(
    report: AdminReport,
    onDismiss: () -> Unit,
    onResolve: (status: String, ban: BanDuration?) -> Unit
) {
    // null = resolve/dismiss without banning; a value = ban the target for that length first.
    var banDuration by remember(report.id) { mutableStateOf<BanDuration?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.admin_report_actions_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(
                    R.string.admin_report_target,
                    report.targetHandle ?: report.targetId
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.admin_report_reason, reasonLabel(report.reason)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (report.targetBanCount > 0) {
                Text(
                    text = stringResource(R.string.admin_ban_count, report.targetBanCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = stringResource(R.string.admin_report_ban_label),
                style = MaterialTheme.typography.titleSmall
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem(
                    label = stringResource(R.string.admin_report_ban_none),
                    selected = banDuration == null,
                    onClick = { banDuration = null }
                )
                BanDuration.entries.forEach { duration ->
                    FilterChipItem(
                        label = stringResource(duration.labelRes),
                        selected = banDuration == duration,
                        onClick = { banDuration = duration }
                    )
                }
            }

            AppButton(
                text = stringResource(R.string.admin_report_resolve),
                onClick = { onResolve("resolved", banDuration) },
                modifier = Modifier.fillMaxWidth()
            )
            AppButton(
                text = stringResource(R.string.admin_report_dismiss),
                onClick = { onResolve("dismissed", banDuration) },
                variant = AppButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminReportsContentPreview() {
    FoodTrackerTheme {
        AdminReportsContent(
            state = AdminReportsUiState(
                reports = listOf(
                    AdminReport(
                        id = "1",
                        reporterId = "r1",
                        reporterHandle = "an.nguyen",
                        targetId = "t1",
                        targetHandle = "spammer",
                        reason = "spam",
                        status = "pending",
                        createdAt = "2026-06-24T10:30:00+00:00"
                    )
                )
            ),
            onBack = {},
            onFilterChange = {},
            onRetry = {},
            onLoadMore = {},
            onReportClick = {}
        )
    }
}
