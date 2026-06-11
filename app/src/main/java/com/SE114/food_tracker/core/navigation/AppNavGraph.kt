package com.SE114.food_tracker.core.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.SE114.food_tracker.feature.auth.ForgotPasswordScreen
import com.SE114.food_tracker.feature.auth.LoginScreen
import com.SE114.food_tracker.feature.auth.RegisterScreen
import com.SE114.food_tracker.feature.auth.SplashScreen
import com.SE114.food_tracker.feature.diary.DiaryScreen
import com.SE114.food_tracker.feature.stats.StatisticsScreen
import com.SE114.food_tracker.feature.chat.ChatScreen
import com.SE114.food_tracker.feature.chat.ConversationListScreen

object NavGraphs {
    const val AUTH = "auth_graph"
    const val MAIN = "main_graph"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    // One-shot signal: set true by the FAB, DiaryScreen consumes it and resets via callback
    triggerDiaryAdd: Boolean = false,
    onDiaryAddHandled: () -> Unit = {}
) {
    fun enterMainClearingAuth() {
        navController.navigate(NavGraphs.MAIN) {
            popUpTo(NavGraphs.AUTH) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavGraphs.AUTH,
        modifier = modifier
    ) {
        navigation(startDestination = AppDestinations.Splash.route, route = NavGraphs.AUTH) {
            composable(AppDestinations.Splash.route) {
                SplashScreen(
                    onAuthenticated = ::enterMainClearingAuth,
                    onUnauthenticated = {
                        navController.navigate(AppDestinations.Login.route) {
                            popUpTo(AppDestinations.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(AppDestinations.Login.route) {
                LoginScreen(
                    onLoginSuccess = ::enterMainClearingAuth,
                    onNavigateRegister = { navController.navigate(AppDestinations.Register.route) },
                    onNavigateForgot = { navController.navigate(AppDestinations.Forgot.route) }
                )
            }
            composable(AppDestinations.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = ::enterMainClearingAuth,
                    onNavigateLogin = { navController.popBackStack() }
                )
            }
            composable(AppDestinations.Forgot.route) {
                ForgotPasswordScreen(onBack = { navController.popBackStack() })
            }
        }

        navigation(startDestination = AppDestinations.Diary.route, route = NavGraphs.MAIN) {
            composable(AppDestinations.Diary.route) {
                DiaryScreen(
                    triggerAdd = triggerDiaryAdd,
                    onAddTriggered = onDiaryAddHandled
                )
            }
            composable(AppDestinations.Stats.route)    { StatisticsScreen() }
            // ĐẤU NỐI TẠM THỜI: Bẻ hướng Tab Feed (Tab 3) để hiển thị danh sách chat
            composable(AppDestinations.Feed.route)     {
                val chatTestViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.SE114.food_tracker.feature.chat.ChatViewModel>()
                ConversationListScreen(
                    viewModel = chatTestViewModel,
                    onConversationClick = { conversationId, conversationName ->
                        navController.navigate("chat_screen/$conversationId/$conversationName")
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(AppDestinations.Chat.route)     {
                ConversationListScreen(
                    onConversationClick = { conversationId, conversationName ->
                        navController.navigate("chat_screen/$conversationId/$conversationName")
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("chat_screen/{conversationId}/{conversationName}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("conversationId") ?: ""
                val name = backStackEntry.arguments?.getString("conversationName") ?: "Người dùng"

                val chatDetailViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.SE114.food_tracker.feature.chat.ChatViewModel>()

                ChatScreen(
                    conversationId = id,
                    conversationName = name,
                    viewModel = chatDetailViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(AppDestinations.Settings.route) { Text("TODO: Settings (TV5)") }
        }
    }
}
