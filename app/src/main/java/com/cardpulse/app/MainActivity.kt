package com.cardpulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cardpulse.app.ui.screen.AddCardScreen
import com.cardpulse.app.ui.screen.CardDetailScreen
import com.cardpulse.app.ui.screen.DashboardScreen
import com.cardpulse.app.ui.theme.CardPulseTheme

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

    NavHost(navController = navController, startDestination = "dashboard") {
        // Dashboard
        composable("dashboard") {
            DashboardScreen(navController)
        }

        // Add/Edit Card - Updated to accept optional cardId
        composable(
            route = "add_card/{cardId}",
            arguments = listOf(
                navArgument("cardId") {
                    type = NavType.LongType
                    defaultValue = -1L  // -1 means "add new card"
                }
            )
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId")?.takeIf { it != -1L }
            AddCardScreen(navController, editCardId = cardId)
        }

        // Card Detail
        composable(
            route = "card_detail/{cardId}",
            arguments = listOf(
                navArgument("cardId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId") ?: return@composable
            CardDetailScreen(navController, cardId)
        }
    }
}