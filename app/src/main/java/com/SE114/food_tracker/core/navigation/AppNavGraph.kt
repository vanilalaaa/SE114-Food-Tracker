package com.SE114.food_tracker.core.navigation

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.SE114.food_tracker.feature.auth.ChangePasswordScreen
import com.SE114.food_tracker.feature.auth.CompleteProfileScreen
import com.SE114.food_tracker.feature.auth.ForgotPasswordScreen
import com.SE114.food_tracker.feature.auth.LoginScreen
import com.SE114.food_tracker.feature.auth.PostAuthDestination
import com.SE114.food_tracker.feature.auth.RegisterScreen
import com.SE114.food_tracker.feature.auth.SplashScreen
import com.SE114.food_tracker.feature.auth.VerifyEmailScreen
import com.SE114.food_tracker.feature.diary.DiaryScreen
import com.SE114.food_tracker.feature.profile.MyProfileScreen
import com.SE114.food_tracker.feature.settings.CategoryManagementScreen
import com.SE114.food_tracker.feature.settings.SettingsScreen
import com.SE114.food_tracker.feature.stats.StatisticsScreen
import com.SE114.food_tracker.feature.feed.FeedScreen
import com.SE114.food_tracker.feature.friend.FriendScreen
import com.SE114.food_tracker.feature.chat.ChatScreen
import com.SE114.food_tracker.feature.chat.ConversationListScreen
import com.SE114.food_tracker.feature.profile.ProfileScreen
import com.SE114.food_tracker.feature.admin.AdminDashboardScreen
import com.SE114.food_tracker.feature.admin.AdminReportsScreen
import com.SE114.food_tracker.feature.admin.AdminUsersScreen

object NavGraphs {
    const val AUTH = "auth_graph"
    const val MAIN = "main_graph"
    const val ADMIN = "admin_graph"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    // One-shot signal: set true by the FAB, DiaryScreen consumes it and resets via callback
    triggerDiaryAdd: Boolean = false,
    onDiaryAddHandled: () -> Unit = {}
) {
    fun enterMain() {
        navController.navigate(NavGraphs.MAIN) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    fun goToCompleteProfile() {
        navController.navigate(AppDestinations.CompleteProfile.route) {
            popUpTo(NavGraphs.AUTH) { inclusive = true }
        }
    }

    fun navigatePostAuth(destination: PostAuthDestination) = when (destination) {
        PostAuthDestination.Diary -> enterMain()
        PostAuthDestination.CompleteProfile -> goToCompleteProfile()
    }

    NavHost(
        navController = navController,
        startDestination = NavGraphs.AUTH,
        modifier = modifier
    ) {
        navigation(startDestination = AppDestinations.Splash.route, route = NavGraphs.AUTH) {
            composable(AppDestinations.Splash.route) {
                SplashScreen(
                    onResolved = ::navigatePostAuth,
                    onUnauthenticated = {
                        navController.navigate(AppDestinations.Login.createRoute()) {
                            popUpTo(AppDestinations.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = AppDestinations.Login.route,
                arguments = listOf(
                    navArgument(AppDestinations.Login.ARG_REASON) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                LoginScreen(
                    onAuthenticated = ::navigatePostAuth,
                    onNavigateRegister = { navController.navigate(AppDestinations.Register.route) },
                    onNavigateForgot = { navController.navigate(AppDestinations.Forgot.route) }
                )
            }
            composable(AppDestinations.Register.route) {
                RegisterScreen(
                    onAuthenticated = ::navigatePostAuth,
                    onNavigateToVerifyEmail = { email, displayName, userId ->
                        navController.navigate(AppDestinations.VerifyEmail.createRoute(email, displayName, userId))
                    },
                    onNavigateLogin = { navController.popBackStack() }
                )
            }
            composable(AppDestinations.VerifyEmail.route) {
                VerifyEmailScreen(
                    onVerified = ::enterMain,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppDestinations.Forgot.route) {
                ForgotPasswordScreen(onBack = { navController.popBackStack() })
            }
        }

        composable(AppDestinations.CompleteProfile.route) {
            CompleteProfileScreen(onComplete = ::enterMain)
        }

        navigation(startDestination = AppDestinations.Diary.route, route = NavGraphs.MAIN) {
            composable(AppDestinations.Diary.route) {
                DiaryScreen(
                    triggerAdd = triggerDiaryAdd,
                    onAddTriggered = onDiaryAddHandled
                )
            }
            composable(AppDestinations.Stats.route)    { StatisticsScreen() }

            composable(AppDestinations.Feed.route) {
                FeedScreen(
                    onNavigateToFriend = { navController.navigate(AppDestinations.Friend.route) },
                    onNavigateToProfile = { profileId ->
                        navController.navigate(AppDestinations.Profile.createRoute(profileId))
                    }
                )
            }

            composable(AppDestinations.Friend.route) {
                FriendScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProfile = { profileId ->
                        navController.navigate(AppDestinations.Profile.createRoute(profileId))
                    }
                )
            }

            composable(AppDestinations.Profile.route) { backStackEntry ->
                ProfileScreen(
                    profileId = backStackEntry.arguments?.getString("profileId").orEmpty(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(AppDestinations.Chat.route)     {
                ConversationListScreen(
                    onConversationClick = { conversationId, conversationName ->
                        navController.navigate(
                            "chat_screen/$conversationId/${Uri.encode(conversationName)}"
                        )
                    }
                )
            }
            composable("chat_screen/{conversationId}/{conversationName}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("conversationId") ?: ""
                val name = backStackEntry.arguments?.getString("conversationName") ?: "Người dùng"

                val chatDetailViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.SE114.food_tracker.feature.chat.ChatViewModel>(
                    key = id
                )
                ChatScreen(
                    conversationId = id,
                    conversationName = name,
                    viewModel = chatDetailViewModel,
                    onBackClick = { navController.popBackStack() },
                    onWalletClick = {
                        navController.navigate("group_wallet_screen/$id")
                    }
                )
            }
//            composable("group_wallet_screen/{conversationId}") { backStackEntry ->
//                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
//
//                GroupWalletScreen(
//                    conversationId = conversationId,
//                    onBackClick = { navController.popBackStack() }
//                )
//            }

            composable(AppDestinations.Settings.route) {
                SettingsScreen(
                    onNavigateToProfile = { navController.navigate(AppDestinations.MyProfile.route) },
                    onNavigateToCategories = { navController.navigate(AppDestinations.CategoryManagement.route) },
                    onChangePassword = { navController.navigate(AppDestinations.ChangePassword.route) },
                    onNavigateToAdmin = { navController.navigate(AppDestinations.AdminDashboard.route) }
                )
            }

            composable(AppDestinations.ChangePassword.route) {
                ChangePasswordScreen(onBack = { navController.popBackStack() })
            }

            composable(AppDestinations.MyProfile.route) {
                MyProfileScreen(onBack = { navController.popBackStack() })
            }

            composable(AppDestinations.CategoryManagement.route) {
                CategoryManagementScreen(onBack = { navController.popBackStack() })
            }
        }

        navigation(startDestination = AppDestinations.AdminDashboard.route, route = NavGraphs.ADMIN) {
            composable(AppDestinations.AdminDashboard.route) {
                AdminDashboardScreen(
                    onNavigateToUsers = { navController.navigate(AppDestinations.AdminUsers.route) },
                    onNavigateToReports = { navController.navigate(AppDestinations.AdminReports.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppDestinations.AdminUsers.route) {
                AdminUsersScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestinations.AdminReports.route) {
                AdminReportsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
