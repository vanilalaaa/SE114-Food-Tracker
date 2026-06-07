package com.SE114.food_tracker.core

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.SE114.food_tracker.core.navigation.AppDestinations
import com.SE114.food_tracker.core.navigation.AppNavGraph
import com.SE114.food_tracker.core.designsystem.components.BottomBar
import com.SE114.food_tracker.feature.diary.components.AddActionButton

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    var selectedIndex by remember { mutableIntStateOf(0) }

    val barRoutes = listOf(
        AppDestinations.Diary.route,
        AppDestinations.Stats.route,
        AppDestinations.Feed.route,
        AppDestinations.Settings.route
    )

    // Track the current route so the FAB only appears on the Diary tab
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    // FAB state lives here; DiaryScreen receives a callback to trigger the add flow
    var triggerDiaryAdd by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            BottomBar(
                selectedIndex = selectedIndex,
                onItemSelected = { index ->
                    selectedIndex = index
                    navController.navigate(barRoutes[index]) {
                        popUpTo(AppDestinations.Diary.route) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                }
            )
        },
        floatingActionButton = {
            // FAB only visible on the Diary destination
            if (currentRoute == AppDestinations.Diary.route) {
                AddActionButton(onClick = { triggerDiaryAdd = true })
            }
        }
    ) { padding ->
        AppNavGraph(
            navController    = navController,
            modifier         = Modifier.padding(padding),
            triggerDiaryAdd  = triggerDiaryAdd,
            onDiaryAddHandled = { triggerDiaryAdd = false }
        )
    }
}