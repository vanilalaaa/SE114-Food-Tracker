package com.SE114.food_tracker.core.navigation

sealed class AppDestinations(val route: String) {
    data object Diary : AppDestinations("diary")
    data object Stats : AppDestinations("stats")
    data object Feed : AppDestinations("feed")
    data object Chat : AppDestinations("chat")
    data object Settings : AppDestinations("settings")
}
