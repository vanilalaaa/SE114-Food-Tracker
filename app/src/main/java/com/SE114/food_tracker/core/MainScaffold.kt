package com.SE114.food_tracker.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.SE114.food_tracker.core.designsystem.components.BottomBar
import com.SE114.food_tracker.core.navigation.AppDestinations
import com.SE114.food_tracker.core.navigation.AppNavGraph
import com.SE114.food_tracker.core.util.CurrencyDisplay
import com.SE114.food_tracker.core.util.LocalCurrencyDisplay
import com.SE114.food_tracker.feature.settings.CurrencyViewModel
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
        AppDestinations.CategoryManagement.route -> AppDestinations.Settings.route
        AppDestinations.ChangePassword.route -> AppDestinations.Settings.route
        CHAT_DETAIL_ROUTE -> AppDestinations.Chat.route
        else -> route
    }

private val AUTH_ROUTES = setOf(
    AppDestinations.Splash.route,
    AppDestinations.Login.route,
    AppDestinations.Register.route,
    AppDestinations.Forgot.route,
    // Pre-auth: no session exists until OTP verification, so the session guard must not bounce it.
    AppDestinations.VerifyEmail.route
)

// complete_profile is post-auth but pre-onboarding: no bottom bar, but a dropped
// session there should still bounce to login (so it stays out of the guard skip set).
// The admin graph is a separate area with its own back navigation — no bottom bar.
private val NO_BOTTOM_BAR_ROUTES = AUTH_ROUTES + setOf(
    AppDestinations.CompleteProfile.route,
    AppDestinations.AdminDashboard.route,
    AppDestinations.AdminUsers.route,
    AppDestinations.AdminReports.route
)

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    val session by mainViewModel.sessionStatus.collectAsStateWithLifecycle()
    val blockedReason by mainViewModel.blockedReason.collectAsStateWithLifecycle()

    // Re-verify the account whenever the app returns to the foreground (e.g. an admin on
    // another device just banned this user).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) mainViewModel.recheckActive()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // App-wide display-currency context: every price formats through this, so switching
    // the currency in Settings re-renders all amounts without mutating any stored value.
    val currencyViewModel: CurrencyViewModel = hiltViewModel()
    val currencyState by currencyViewModel.uiState.collectAsStateWithLifecycle()
    val currencyDisplay = remember(currencyState.displayCurrency, currencyState.rates) {
        CurrencyDisplay(currencyState.displayCurrency, currencyState.rates)
    }

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val selectedRoute = bottomBarRouteFor(currentRoute)
    val selectedIndex = BAR_ROUTES.indexOf(selectedRoute).coerceAtLeast(0)
    val showBottomBar = currentRoute != null && currentRoute !in NO_BOTTOM_BAR_ROUTES
    val onBottomItemSelected: (Int) -> Unit = { index ->
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

    // Auth guard: if the session drops while inside the app, return to login and clear the back
    // stack. A non-null blockedReason (banned/soft-deleted sign-out) is carried to the Login screen.
    LaunchedEffect(session, currentRoute, blockedReason) {
        if (session is SessionStatus.NotAuthenticated && currentRoute != null && currentRoute !in AUTH_ROUTES) {
            navController.navigate(AppDestinations.Login.createRoute(blockedReason)) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    CompositionLocalProvider(LocalCurrencyDisplay provides currencyDisplay) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AppNavGraph(
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )
            if (showBottomBar) {
                BottomBar(
                    selectedIndex = selectedIndex,
                    onItemSelected = onBottomItemSelected,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
    }
}
