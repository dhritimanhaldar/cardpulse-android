package com.cardpulse.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cardpulse.app.ui.screen.AddCardScreen
import com.cardpulse.app.ui.screen.CardDetailScreen
import com.cardpulse.app.ui.screen.DashboardScreen
import com.cardpulse.app.ui.screen.LoginScreen
import com.cardpulse.app.viewmodel.DashboardViewModel

sealed class Screen(val route: String) {
    object Login     : Screen("login")
    object Dashboard : Screen("dashboard")
    object CardDetail : Screen("card_detail/{cardId}") {
        fun createRoute(cardId: Int) = "card_detail/$cardId"
    }
    object AddCard : Screen("add_card")
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
            val context = LocalContext.current
            val dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(context))
            DashboardScreen(
                onCardClick = { cardId ->
                    navController.navigate(Screen.CardDetail.createRoute(cardId))
                },
                onAddCard = {
                    navController.navigate(Screen.AddCard.route)
                }
            )
        }

        composable(Screen.CardDetail.route) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId")?.toIntOrNull() ?: return@composable
            CardDetailScreen(
                cardId = cardId,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() }
            )
        }

        composable(Screen.AddCard.route) {
            val context = LocalContext.current
            // Use the same ViewModel instance as Dashboard by targeting the parent back stack entry
            val dashboardEntry = remember(navController) {
                navController.getBackStackEntry(Screen.Dashboard.route)
            }
            val dashboardViewModel: DashboardViewModel = viewModel(
                viewModelStoreOwner = dashboardEntry,
                factory = DashboardViewModel.factory(context)
            )
            AddCardScreen(
                onSave = { card ->
                    dashboardViewModel.addCard(card)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
