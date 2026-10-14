package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WangiDarkColorScheme = darkColorScheme(
    primary = WangiRed,
    onPrimary = WangiWhite,
    primaryContainer = WangiDarkRed,
    onPrimaryContainer = WangiWhite,
    secondary = WangiGold,
    onSecondary = WangiCharcoal,
    secondaryContainer = WangiGoldDark,
    onSecondaryContainer = WangiWhite,
    tertiary = WangiGold,
    background = WangiDarkBackground,
    onBackground = WangiWhite,
    surface = WangiDarkSurface,
    onSurface = WangiWhite,
    surfaceVariant = WangiDarkBorder,
    onSurfaceVariant = WangiTextMuted,
    outline = WangiDarkBorder
)

private val WangiLightColorScheme = lightColorScheme(
    primary = WangiRed,
    onPrimary = WangiWhite,
    primaryContainer = WangiLightRed,
    onPrimaryContainer = WangiDarkRed,
    secondary = WangiCharcoal,
    onSecondary = WangiWhite,
    secondaryContainer = WangiSurfaceVariant,
    onSecondaryContainer = WangiCharcoal,
    tertiary = WangiGold,
    onTertiary = WangiCharcoal,
    tertiaryContainer = WangiGoldLight,
    onTertiaryContainer = WangiGoldDark,
    background = WangiBackground,
    onBackground = WangiTextPrimary,
    surface = WangiSurface,
    onSurface = WangiTextPrimary,
    surfaceVariant = WangiSurfaceVariant,
    onSurfaceVariant = WangiTextSecondary,
    outline = WangiBorder
)

@Composable
fun WangiTechTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) WangiDarkColorScheme else WangiLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
