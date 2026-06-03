package com.SE114.food_tracker.core

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.SE114.food_tracker.core.navigation.AppDestinations
import com.SE114.food_tracker.core.navigation.AppNavGraph
import com.SE114.food_tracker.ui.components.BottomBar

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    var selectedIndex by remember { mutableIntStateOf(0) }

    // BottomBar chỉ phơi ra 4 mục; map theo thứ tự icon hiện có. Đồng bộ đầy đủ back stack ↔ NavController là việc của Sprint 1.
    val barRoutes = listOf(
        AppDestinations.Diary.route,
        AppDestinations.Stats.route,
        AppDestinations.Feed.route,
        AppDestinations.Settings.route
    )

    Scaffold(
        bottomBar = {
            BottomBar(
                selectedIndex = selectedIndex,
                onItemSelected = { index ->
                    selectedIndex = index
                    navController.navigate(barRoutes[index]) {
                        popUpTo(AppDestinations.Diary.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        AppNavGraph(
            navController = navController,
            modifier = Modifier.padding(padding)
        )
    }
}
