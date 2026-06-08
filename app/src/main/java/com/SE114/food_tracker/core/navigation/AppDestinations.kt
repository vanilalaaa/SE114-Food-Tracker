package com.SE114.food_tracker.core.navigation

sealed class AppDestinations(val route: String) {
    data object Splash : AppDestinations("splash")
    data object Login : AppDestinations("login")
    data object Register : AppDestinations("register")
    data object Forgot : AppDestinations("forgot")
    data object Diary : AppDestinations("diary")
    data object Stats : AppDestinations("stats")
    data object Feed : AppDestinations("feed")
    data object Chat : AppDestinations("chat")
    data object Settings : AppDestinations("settings")
}
