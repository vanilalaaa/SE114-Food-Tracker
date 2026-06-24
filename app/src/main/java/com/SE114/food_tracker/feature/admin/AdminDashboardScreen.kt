package com.SE114.food_tracker.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.components.AppButton
import com.SE114.food_tracker.core.designsystem.components.AppScaffold
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.feature.admin.components.AdminTopBar

@Composable
fun AdminDashboardScreen(
    onNavigateToUsers: () -> Unit,
    onNavigateToReports: () -> Unit,
    onBack: () -> Unit
) {
    AdminDashboardContent(
        onNavigateToUsers = onNavigateToUsers,
        onNavigateToReports = onNavigateToReports,
        onBack = onBack
    )
}

@Composable
private fun AdminDashboardContent(
    onNavigateToUsers: () -> Unit,
    onNavigateToReports: () -> Unit,
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

            AppButton(
                text = stringResource(R.string.admin_dashboard_users),
                onClick = onNavigateToUsers,
                modifier = Modifier.fillMaxWidth()
            )
            AppButton(
                text = stringResource(R.string.admin_dashboard_reports),
                onClick = onNavigateToReports,
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
            onNavigateToUsers = {},
            onNavigateToReports = {},
            onBack = {}
        )
    }
}
