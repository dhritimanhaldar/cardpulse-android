package com.cardpulse.app.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── CardPulse Colour Palette ──────────────────────────────────
val PulseBlue       = Color(0xFF1A73E8)
val PulseDarkBlue   = Color(0xFF0D47A1)
val PulseAccent     = Color(0xFF00BCD4)
val PulseGold       = Color(0xFFFFD700)
val PulseBackground = Color(0xFF0A0A0F)
val PulseSurface    = Color(0xFF14141E)
val PulseCard       = Color(0xFF1C1C2E)
val PulseOnSurface  = Color(0xFFE8E8F0)
val PulseSubtext    = Color(0xFF9090A0)
val PulseSuccess    = Color(0xFF4CAF50)
val PulseWarning    = Color(0xFFFF9800)
val PulseDanger     = Color(0xFFE53935)

private val DarkColorScheme = darkColorScheme(
    primary          = PulseBlue,
    onPrimary        = Color.White,
    primaryContainer = PulseDarkBlue,
    secondary        = PulseAccent,
    onSecondary      = Color.Black,
    background       = PulseBackground,
    onBackground     = PulseOnSurface,
    surface          = PulseSurface,
    onSurface        = PulseOnSurface,
    surfaceVariant   = PulseCard,
    error            = PulseDanger,
    onError          = Color.White
)

@Composable
fun CardPulseTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PulseBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography   = Typography(),
        content      = content
    )
}
