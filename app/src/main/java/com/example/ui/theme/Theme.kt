package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CivicGreenAccent,
    onPrimary = CharcoalDark,
    primaryContainer = CivicGreenPrimary,
    onPrimaryContainer = CivicGreenLight,
    secondary = CivicTealCollected,
    onSecondary = Color.White,
    tertiary = AlertMediumOrange,
    background = CharcoalDark,
    onBackground = OffWhiteBackground,
    surface = CharcoalSurface,
    onSurface = OffWhiteBackground,
    surfaceVariant = Color(0xFF333B36),
    onSurfaceVariant = Color(0xFFCFD6D1),
    error = AlertCriticalRed,
    outline = Color(0xFF4A544E)
)

private val LightColorScheme = lightColorScheme(
    primary = CivicGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = CivicGreenLight,
    onPrimaryContainer = CivicGreenPrimary,
    secondary = CivicTealCollected,
    onSecondary = Color.White,
    secondaryContainer = CivicTealLight,
    onSecondaryContainer = CivicTealCollected,
    tertiary = AlertMediumOrange,
    background = OffWhiteBackground,
    onBackground = CharcoalDark,
    surface = OffWhiteSurface,
    onSurface = CharcoalDark,
    surfaceVariant = Color(0xFFECEFEC),
    onSurfaceVariant = CharcoalMuted,
    error = AlertCriticalRed,
    outline = OffWhiteCardBorder
)

@Composable
fun GreenRouteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    GreenRouteTheme(darkTheme = darkTheme, content = content)
}
