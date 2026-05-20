package com.school.timetable.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ModernLightColorScheme = lightColorScheme(
    primary = AccentPrimary,
    secondary = AccentSecondary,
    background = LightBackground,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onPrimary = SurfaceLight,
    onSecondary = TextPrimaryLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderColorLight
)

private val ModernDarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    secondary = AccentSecondary,
    background = DarkBackground,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onPrimary = SurfaceDark,
    onSecondary = TextPrimaryDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderColorDark
)

@Composable
fun SmartTimetableTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) ModernDarkColorScheme else ModernLightColorScheme,
        content = content
    )
}
