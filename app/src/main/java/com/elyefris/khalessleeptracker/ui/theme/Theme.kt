package com.elyefris.khalessleeptracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Definición de colores (igual que antes)
private val DarkColorScheme = darkColorScheme(
    primary = PastelPurple,
    secondary = PastelPink,
    tertiary = PastelBlue,
    background = Color(0xFF1A1A2E) // Fondo oscuro profundo
)

private val LightColorScheme = lightColorScheme(
    primary = PastelPurple,
    secondary = PastelPink,
    tertiary = PastelBlue,
    background = PastelCream
)

@Composable
fun KhalesSleepTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color lo desactivamos para priorizar nuestros pasteles
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // 1. Barra de estado transparente para que luzca el gradiente
            window.statusBarColor = Color.Transparent.toArgb()

            // 2. Control de Iconos (Aquí estaba el detalle)
            // isAppearanceLightStatusBars = true  -> Pone iconos NEGROS (para fondos claros)
            // isAppearanceLightStatusBars = false -> Pone iconos BLANCOS (para fondos oscuros)
            // Por tanto, debe ser '!darkTheme' (Si NO es oscuro, pon iconos negros)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}