package com.SE114.food_tracker.core.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.SE114.food_tracker.feature.auth.ForgotPasswordScreen
import com.SE114.food_tracker.feature.auth.LoginScreen
import com.SE114.food_tracker.feature.auth.RegisterScreen
import com.SE114.food_tracker.feature.auth.SplashScreen
import com.SE114.food_tracker.feature.diary.DiaryScreen
import com.SE114.food_tracker.feature.stats.StatisticsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    // One-shot signal: set true by the FAB, DiaryScreen consumes it and resets via callback
    triggerDiaryAdd: Boolean = false,
    onDiaryAddHandled: () -> Unit = {}
) {
    fun goToDiaryClearingAuth() {
        navController.navigate(AppDestinations.Diary.route) {
            popUpTo(AppDestinations.Splash.route) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppDestinations.Splash.route,
        modifier = modifier
    ) {
        composable(AppDestinations.Splash.route) {
            SplashScreen(
                onAuthenticated = ::goToDiaryClearingAuth,
                onUnauthenticated = {
                    navController.navigate(AppDestinations.Login.route) {
                        popUpTo(AppDestinations.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(AppDestinations.Login.route) {
            LoginScreen(
                onLoginSuccess = ::goToDiaryClearingAuth,
                onNavigateRegister = { navController.navigate(AppDestinations.Register.route) },
                onNavigateForgot = { navController.navigate(AppDestinations.Forgot.route) }
            )
        }
        composable(AppDestinations.Register.route) {
            RegisterScreen(
                onRegisterSuccess = ::goToDiaryClearingAuth,
                onNavigateLogin = { navController.popBackStack() }
            )
        }
        composable(AppDestinations.Forgot.route) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }

        composable(AppDestinations.Diary.route) {
            DiaryScreen(
                triggerAdd = triggerDiaryAdd,
                onAddTriggered = onDiaryAddHandled
            )
        }
        composable(AppDestinations.Stats.route)    { StatisticsScreen() }
        composable(AppDestinations.Feed.route)     { Text("TODO: Feed (TV3)") }
        composable(AppDestinations.Chat.route)     { Text("TODO: Chat (TV4)") }
        composable(AppDestinations.Settings.route) { Text("TODO: Settings (TV5)") }
    }
}
