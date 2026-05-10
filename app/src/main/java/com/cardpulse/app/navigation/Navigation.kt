package com.cardpulse.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cardpulse.app.ui.screen.DashboardScreen
import com.cardpulse.app.ui.screen.LoginScreen

sealed class Screen(val route: String) {
    object Login     : Screen("login")
    object Dashboard : Screen("dashboard")
    object CardDetail : Screen("card_detail/{cardId}") {
        fun createRoute(cardId: Int) = "card_detail/$cardId"
    }
}

@Composable
fun CardPulseNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController    = navController,
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
            DashboardScreen(
                onCardClick = { cardId ->
                    navController.navigate(Screen.CardDetail.createRoute(cardId))
                }
            )
        }

        composable(Screen.CardDetail.route) {
            // Placeholder — will be built in next batch
        }
    }
}
