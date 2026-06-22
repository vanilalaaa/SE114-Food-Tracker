package com.SE114.food_tracker.core

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.SE114.food_tracker.core.designsystem.components.BottomBar
import com.SE114.food_tracker.core.navigation.AppDestinations
import com.SE114.food_tracker.core.navigation.AppNavGraph
import com.SE114.food_tracker.feature.diary.components.AddActionButton
import io.github.jan.supabase.auth.status.SessionStatus

private val BAR_ROUTES = listOf(
    AppDestinations.Diary.route,
    AppDestinations.Stats.route,
    AppDestinations.Feed.route,
    AppDestinations.Chat.route,
    AppDestinations.Settings.route
)

private const val CHAT_DETAIL_ROUTE = "chat_screen/{conversationId}/{conversationName}"

private fun bottomBarRouteFor(route: String?): String? =
    when (route) {
        AppDestinations.Friend.route -> AppDestinations.Feed.route
        AppDestinations.Profile.route -> AppDestinations.Feed.route
        AppDestinations.MyProfile.route -> AppDestinations.Settings.route
        CHAT_DETAIL_ROUTE -> AppDestinations.Chat.route
        else -> route
    }

private val AUTH_ROUTES = setOf(
    AppDestinations.Splash.route,
    AppDestinations.Login.route,
    AppDestinations.Register.route,
    AppDestinations.Forgot.route
)

// complete_profile is post-auth but pre-onboarding: no bottom bar, but a dropped
// session there should still bounce to login (so it stays out of the guard skip set).
private val NO_BOTTOM_BAR_ROUTES = AUTH_ROUTES + AppDestinations.CompleteProfile.route

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    val session by mainViewModel.sessionStatus.collectAsStateWithLifecycle()

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val selectedRoute = bottomBarRouteFor(currentRoute)
    val selectedIndex = BAR_ROUTES.indexOf(selectedRoute).coerceAtLeast(0)
    val showBottomBar = currentRoute != null && currentRoute !in NO_BOTTOM_BAR_ROUTES

    // FAB state lives here; DiaryScreen receives a callback to trigger the add flow
    var triggerDiaryAdd by remember { mutableStateOf(false) }

    // Auth guard: if the session drops while inside the app, return to login and clear the back stack.
    LaunchedEffect(session, currentRoute) {
        if (session is SessionStatus.NotAuthenticated && currentRoute != null && currentRoute !in AUTH_ROUTES) {
            navController.navigate(AppDestinations.Login.route) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    selectedIndex = selectedIndex,
                    onItemSelected = { index ->
                        val targetRoute = BAR_ROUTES[index]
                        if (selectedRoute == targetRoute && currentRoute != targetRoute) {
                            val poppedToTabRoot = navController.popBackStack(
                                route = targetRoute,
                                inclusive = false
                            )
                            if (!poppedToTabRoot) {
                                navController.navigate(targetRoute) {
                                    launchSingleTop = true
                                }
                            }
                        } else {
                            navController.navigate(targetRoute) {
                                // Diary is the main graph's start destination.
                                popUpTo(AppDestinations.Diary.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = targetRoute != AppDestinations.Feed.route
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            // FAB only visible on the Diary destination
            if (currentRoute == AppDestinations.Diary.route) {
                AddActionButton(onClick = { triggerDiaryAdd = true })
            }
        }
    ) { padding ->
        AppNavGraph(
            navController = navController,
            modifier = Modifier.padding(padding),
            triggerDiaryAdd = triggerDiaryAdd,
            onDiaryAddHandled = { triggerDiaryAdd = false }
        )
    }
}
