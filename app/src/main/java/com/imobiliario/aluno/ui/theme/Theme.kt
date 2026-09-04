package com.imobiliario.aluno.ui.theme

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = CapulanaGreen,
    onPrimary = Color.White,
    primaryContainer = CapulanaGreenContainer,
    onPrimaryContainer = CapulanaGreen,
    secondary = CapulanaAmber,
    onSecondary = Color.White,
    secondaryContainer = CapulanaAmberContainer,
    onSecondaryContainer = CapulanaAmber,
    error = CapulanaRed,
    onError = SurfaceLight,
    errorContainer = CapulanaRedContainer,
    onErrorContainer = CapulanaRed,
    background = SurfaceLight,
    onBackground = Neutral10,
    surface = SurfaceLight,
    onSurface = Neutral10,
    surfaceVariant = Neutral95,
    onSurfaceVariant = Neutral20,
    outline = Neutral90,
)

private val DarkColors = darkColorScheme(
    primary = CapulanaGreenLight,
    onPrimary = Neutral10,
    primaryContainer = CapulanaGreen,
    onPrimaryContainer = CapulanaGreenContainer,
    secondary = CapulanaAmber,
    onSecondary = Neutral10,
    secondaryContainer = CapulanaAmber,
    onSecondaryContainer = CapulanaAmberContainer,
    error = Color(0xFFFFB4AB),
    onError = Neutral10,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = CapulanaRedContainer,
    background = SurfaceDark,
    onBackground = Neutral90,
    surface = SurfaceDark,
    onSurface = Neutral90,
    surfaceVariant = Neutral20,
    onSurfaceVariant = Neutral90,
    outline = Neutral20,
)

@Composable
fun MeuFilhoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MeuFilhoTypography,
        shapes = MeuFilhoShapes,
        content = content
    )
}
