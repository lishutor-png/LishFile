package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SoftBluePrimaryDark,
    onPrimary = SoftBlueOnPrimaryDark,
    primaryContainer = SoftBluePrimaryContainerDark,
    onPrimaryContainer = SoftBlueOnPrimaryContainerDark,
    secondary = SoftBlueSecondaryDark,
    onSecondary = SoftBlueOnSecondaryDark,
    secondaryContainer = SoftBlueSecondaryContainerDark,
    onSecondaryContainer = SoftBlueOnSecondaryContainerDark,
    tertiary = SoftTealTertiaryDark,
    onTertiary = SoftTealOnTertiaryDark,
    tertiaryContainer = SoftTealTertiaryContainerDark,
    onTertiaryContainer = SoftTealOnTertiaryContainerDark,
    background = SoftBlueBackgroundDark,
    onBackground = SoftBlueOnBackgroundDark,
    surface = SoftBlueSurfaceDark,
    onSurface = SoftBlueOnSurfaceDark,
    surfaceVariant = SoftBlueSurfaceVariantDark,
    onSurfaceVariant = SoftBlueOnSurfaceVariantDark,
    outline = SoftBlueOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = SoftBluePrimaryLight,
    onPrimary = SoftBlueOnPrimaryLight,
    primaryContainer = SoftBluePrimaryContainerLight,
    onPrimaryContainer = SoftBlueOnPrimaryContainerLight,
    secondary = SoftBlueSecondaryLight,
    onSecondary = SoftBlueOnSecondaryLight,
    secondaryContainer = SoftBlueSecondaryContainerLight,
    onSecondaryContainer = SoftBlueOnSecondaryContainerLight,
    tertiary = SoftTealTertiaryLight,
    onTertiary = SoftTealOnTertiaryLight,
    tertiaryContainer = SoftTealTertiaryContainerLight,
    onTertiaryContainer = SoftTealOnTertiaryContainerLight,
    background = SoftBlueBackgroundLight,
    onBackground = SoftBlueOnBackgroundLight,
    surface = SoftBlueSurfaceLight,
    onSurface = SoftBlueOnSurfaceLight,
    surfaceVariant = SoftBlueSurfaceVariantLight,
    onSurfaceVariant = SoftBlueOnSurfaceVariantLight,
    outline = SoftBlueOutlineLight
)

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Composable
fun LishFileTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    LishFileTheme(
        themeMode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT,
        content = content
    )
}

