package com.cardpulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cardpulse.app.auth.AuthManager
import com.cardpulse.app.ui.screen.AddCardScreen
import com.cardpulse.app.ui.screen.CardDetailScreen
import com.cardpulse.app.ui.screen.DashboardScreen
import com.cardpulse.app.ui.screen.LoginScreen
import com.cardpulse.app.ui.theme.CardPulseTheme
import com.cardpulse.app.viewmodel.DashboardViewModel
import com.cardpulse.app.viewmodel.GmailSyncViewModel
import com.cardpulse.app.worker.scheduleSmsSync
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CardPulseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CardPulseApp()
                }
            }
        }
    }
}

@Composable
fun CardPulseApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val startDestination = if (authManager.isSignedIn) "dashboard" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            val dashboardViewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.factory(navController.context)
            )
            val gmailSyncViewModel: GmailSyncViewModel = viewModel()
            LaunchedEffect(Unit) {
                scheduleSmsSync(navController.context)
                gmailSyncViewModel.autoSyncOnce()
            }
            DashboardScreen(
                navController = navController,
                viewModel = dashboardViewModel
            )
        }

        composable(
            route = "add_card/{cardId}",
            arguments = listOf(
                navArgument("cardId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId")?.takeIf { it != -1L }
            AddCardScreen(navController = navController, editCardId = cardId)
        }

        composable(
            route = "card_detail/{cardId}",
            arguments = listOf(
                navArgument("cardId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId") ?: return@composable
            CardDetailScreen(navController = navController, cardId = cardId)
        }
    }
}
