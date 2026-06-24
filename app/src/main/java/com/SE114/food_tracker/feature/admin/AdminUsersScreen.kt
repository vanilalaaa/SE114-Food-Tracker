package com.SE114.food_tracker.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.components.AppScaffold
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.feature.admin.components.AdminTopBar

@Composable
fun AdminUsersScreen(onBack: () -> Unit) {
    AdminUsersContent(onBack = onBack)
}

@Composable
private fun AdminUsersContent(onBack: () -> Unit) {
    AppScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AdminTopBar(title = stringResource(R.string.admin_users_title), onBack = onBack)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminUsersContentPreview() {
    FoodTrackerTheme {
        AdminUsersContent(onBack = {})
    }
}
