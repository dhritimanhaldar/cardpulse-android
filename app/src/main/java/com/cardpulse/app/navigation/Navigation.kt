package com.cardpulse.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cardpulse.app.ui.screen.AddCardScreen
import com.cardpulse.app.ui.screen.CardDetailScreen
import com.cardpulse.app.ui.screen.DashboardScreen
import com.cardpulse.app.ui.screen.LoginScreen
import com.cardpulse.app.viewmodel.DashboardViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object CardDetail : Screen("card_detail/{cardId}") {
        fun createRoute(cardId: Long) = "card_detail/$cardId"
    }
    object AddCard : Screen("add_card/{cardId}") {
        fun createRoute(cardId: Long = -1L) = "add_card/$cardId"
    }
}

@Composable
fun CardPulseNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val context = LocalContext.current
            val dashboardViewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.factory(context)
            )
            DashboardScreen(
                navController = navController,
                viewModel = dashboardViewModel
            )
        }

        composable(Screen.CardDetail.route) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId")?.toLongOrNull()
                ?: return@composable
            CardDetailScreen(
                navController = navController,
                cardId = cardId
            )
        }

        composable(
            route = Screen.AddCard.route,
            arguments = listOf(
                navArgument("cardId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments
                ?.getLong("cardId")
                ?.takeIf { it != -1L }
            AddCardScreen(
                navController = navController,
                editCardId = cardId
            )
        }
    }
}
