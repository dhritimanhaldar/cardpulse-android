package com.cardpulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cardpulse.app.auth.AuthManager
import com.cardpulse.app.navigation.CardPulseNavHost
import com.cardpulse.app.navigation.Screen
import com.cardpulse.app.ui.theme.CardPulseTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authManager   = AuthManager(this)
        val startDestination = if (authManager.isSignedIn) {
            Screen.Dashboard.route
        } else {
            Screen.Login.route
        }

        setContent {
            CardPulseTheme {
                CardPulseNavHost(startDestination = startDestination)
            }
        }
    }
}
