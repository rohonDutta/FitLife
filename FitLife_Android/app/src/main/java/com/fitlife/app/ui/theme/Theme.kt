package com.fitlife.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Brand colours
val FitBlue    = Color(0xFF1E88E5)
val FitTeal    = Color(0xFF00897B)
val FitOrange  = Color(0xFFF4511E)
val FitGreen   = Color(0xFF43A047)
val FitPurple  = Color(0xFF7B1FA2)
val FitRed     = Color(0xFFE53935)

private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFF90CAF9),
    onPrimary        = Color(0xFF0D2137),
    primaryContainer = Color(0xFF1565C0),
    secondary        = Color(0xFF80CBC4),
    tertiary         = Color(0xFFFFCC02),
    background       = Color(0xFF0E1621),
    surface          = Color(0xFF182130),
    surfaceVariant   = Color(0xFF1E2D40),
    onBackground     = Color(0xFFE3EDF8),
    onSurface        = Color(0xFFE3EDF8),
    outline          = Color(0xFF2F4460),
    error            = Color(0xFFEF9A9A),
)

private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF1565C0),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFDEECFD),
    secondary        = Color(0xFF00695C),
    tertiary         = Color(0xFFF57F17),
    background       = Color(0xFFF4F6F9),
    surface          = Color.White,
    surfaceVariant   = Color(0xFFEEF2F8),
    onBackground     = Color(0xFF0D1B2A),
    onSurface        = Color(0xFF0D1B2A),
    outline          = Color(0xFFBCC8D8),
)

@Composable
fun FitLifeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
