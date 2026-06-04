package com.SE114.food_tracker.core.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.SE114.food_tracker.feature.diary.DiaryScreen
import com.SE114.food_tracker.feature.stats.StatisticsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.Diary.route,
        modifier = modifier
    ) {
        composable(AppDestinations.Diary.route) { DiaryScreen() }
        composable(AppDestinations.Stats.route) { StatisticsScreen() }
        composable(AppDestinations.Feed.route) { Text("TODO: Feed (TV3)") }
        composable(AppDestinations.Chat.route) { Text("TODO: Chat (TV4)") }
        composable(AppDestinations.Settings.route) { Text("TODO: Settings (TV5)") }
    }
}
