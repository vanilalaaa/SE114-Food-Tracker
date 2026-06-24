package com.SE114.food_tracker.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.SE114.food_tracker.data.repository.AdminDashboardStats
import com.SE114.food_tracker.feature.admin.components.AdminStatCard
import com.SE114.food_tracker.feature.admin.components.AdminTopBar

@Composable
fun AdminDashboardScreen(
    onNavigateToUsers: () -> Unit,
    onNavigateToReports: () -> Unit,
    onBack: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AdminDashboardContent(
        state = state,
        onNavigateToUsers = onNavigateToUsers,
        onNavigateToReports = onNavigateToReports,
        onRetry = viewModel::load,
        onBack = onBack
    )
}

@Composable
private fun AdminDashboardContent(
    state: AdminDashboardUiState,
    onNavigateToUsers: () -> Unit,
    onNavigateToReports: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    AppScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AdminTopBar(title = stringResource(R.string.admin_dashboard_title), onBack = onBack)

            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                state.error != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = state.error.adminMessage(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    AppButton(
                        text = stringResource(R.string.admin_retry),
                        onClick = onRetry,
                        variant = AppButtonVariant.Secondary
                    )
                }
                else -> {
                    val stats = state.stats ?: AdminDashboardStats(0, 0, 0, 0)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AdminStatCard(
                            label = stringResource(R.string.admin_stat_total_users),
                            value = stats.totalUsers.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        AdminStatCard(
                            label = stringResource(R.string.admin_stat_banned),
                            value = stats.bannedCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AdminStatCard(
                            label = stringResource(R.string.admin_stat_deleted),
                            value = stats.deletedCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        AdminStatCard(
                            label = stringResource(R.string.admin_stat_pending_reports),
                            value = stats.pendingReports.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            AppButton(
                text = stringResource(R.string.admin_dashboard_users),
                onClick = onNavigateToUsers,
                modifier = Modifier.fillMaxWidth()
            )
            AppButton(
                text = stringResource(R.string.admin_dashboard_reports),
                onClick = onNavigateToReports,
                variant = AppButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminDashboardContentPreview() {
    FoodTrackerTheme {
        AdminDashboardContent(
            state = AdminDashboardUiState(
                stats = AdminDashboardStats(totalUsers = 128, bannedCount = 3, deletedCount = 1, pendingReports = 5),
                isLoading = false
            ),
            onNavigateToUsers = {},
            onNavigateToReports = {},
            onRetry = {},
            onBack = {}
        )
    }
}
