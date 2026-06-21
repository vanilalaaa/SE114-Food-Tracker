package com.SE114.food_tracker.core.navigation

sealed class AppDestinations(val route: String) {
    data object Splash : AppDestinations("splash")
    data object Login : AppDestinations("login")
    data object Register : AppDestinations("register")
    data object Forgot : AppDestinations("forgot")
    data object CompleteProfile : AppDestinations("complete_profile")
    data object Diary : AppDestinations("diary")
    data object Stats : AppDestinations("stats")
    data object Feed : AppDestinations("feed")
    data object Friend : AppDestinations("friend")
    data object Chat : AppDestinations("chat")
    data object Settings : AppDestinations("settings")
    data object Profile : AppDestinations("profile/me")
}
