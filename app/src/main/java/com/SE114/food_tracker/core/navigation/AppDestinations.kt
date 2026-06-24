package com.SE114.food_tracker.core.navigation

import android.net.Uri

sealed class AppDestinations(val route: String) {
    data object Splash : AppDestinations("splash")
    data object Login : AppDestinations("login")
    data object Register : AppDestinations("register")
    data object Forgot : AppDestinations("forgot")
    data object VerifyEmail : AppDestinations("verify_email/{email}/{displayName}/{userId}") {
        fun createRoute(email: String, displayName: String, userId: String): String =
            "verify_email/${Uri.encode(email)}/${Uri.encode(displayName)}/${Uri.encode(userId)}"
    }
    data object CompleteProfile : AppDestinations("complete_profile")
    data object Diary : AppDestinations("diary")
    data object Stats : AppDestinations("stats")
    data object Feed : AppDestinations("feed")
    data object Friend : AppDestinations("friend")
    data object Profile : AppDestinations("profile/{profileId}") {
        fun createRoute(profileId: String): String = "profile/${Uri.encode(profileId)}"
    }
    data object Chat : AppDestinations("chat")
    data object Settings : AppDestinations("settings")
    data object MyProfile : AppDestinations("my_profile")
    data object CategoryManagement : AppDestinations("category_management")
    data object ChangePassword : AppDestinations("change_password")

    // Admin graph (hidden; reached only via the Login secret code + an is_admin session).
    data object AdminDashboard : AppDestinations("admin/dashboard")
    data object AdminUsers : AppDestinations("admin/users")
    data object AdminReports : AppDestinations("admin/reports")
}
