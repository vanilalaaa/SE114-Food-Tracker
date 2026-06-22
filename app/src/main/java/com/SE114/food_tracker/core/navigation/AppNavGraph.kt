package com.SE114.food_tracker.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.SE114.food_tracker.feature.auth.CompleteProfileScreen
import com.SE114.food_tracker.feature.auth.ForgotPasswordScreen
import com.SE114.food_tracker.feature.auth.LoginScreen
import com.SE114.food_tracker.feature.auth.PostAuthDestination
import com.SE114.food_tracker.feature.auth.RegisterScreen
import com.SE114.food_tracker.feature.auth.SplashScreen
import com.SE114.food_tracker.feature.diary.DiaryScreen
import com.SE114.food_tracker.feature.profile.MyProfileScreen
import com.SE114.food_tracker.feature.settings.SettingsScreen
import com.SE114.food_tracker.feature.stats.StatisticsScreen
import com.SE114.food_tracker.feature.feed.FeedScreen
import com.SE114.food_tracker.feature.friend.FriendScreen
import com.SE114.food_tracker.feature.chat.ChatScreen
import com.SE114.food_tracker.feature.chat.ConversationListScreen
import com.SE114.food_tracker.feature.profile.ProfileScreen

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
                        navController.navigate(AppDestinations.Login.route) {
                            popUpTo(AppDestinations.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(AppDestinations.Login.route) {
                LoginScreen(
                    onAuthenticated = ::navigatePostAuth,
                    onNavigateRegister = { navController.navigate(AppDestinations.Register.route) },
                    onNavigateForgot = { navController.navigate(AppDestinations.Forgot.route) }
                )
            }
            composable(AppDestinations.Register.route) {
                RegisterScreen(
                    onAuthenticated = ::navigatePostAuth,
                    onNavigateLogin = { navController.popBackStack() }
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
                    onNavigateBack = { navController.popBackStack() }
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

                val chatDetailViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.SE114.food_tracker.feature.chat.ChatViewModel>()

                ChatScreen(
                    conversationId = id,
                    conversationName = name,
                    viewModel = chatDetailViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(AppDestinations.Settings.route) {
                SettingsScreen(
                    onNavigateToProfile = { navController.navigate(AppDestinations.MyProfile.route) }
                )
            }

            composable(AppDestinations.MyProfile.route) {
                MyProfileScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
