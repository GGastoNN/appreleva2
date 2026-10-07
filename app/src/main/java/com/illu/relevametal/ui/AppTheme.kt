package com.illu.relevametal.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.illu.relevametal.personalization.AppThemeMode
import com.illu.relevametal.personalization.DEFAULT_ACCENT
import com.illu.relevametal.personalization.PersonalizationSettings
import com.illu.relevametal.personalization.colorInt

private val LightColors = lightColorScheme(
    primary = Color(0xFF1769E0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF001A42),
    secondary = Color(0xFF556579),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E3F3),
    onSecondaryContainer = Color(0xFF121C2A),
    tertiary = Color(0xFF8B4A40),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD4),
    onTertiaryContainer = Color(0xFF3A0905),
    background = Color(0xFFF5F7FB),
    onBackground = Color(0xFF171B22),
    surface = Color(0xFFFBFCFF),
    onSurface = Color(0xFF171B22),
    surfaceVariant = Color(0xFFE4E8F0),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF00468A),
    onPrimaryContainer = Color(0xFFD7E3FF),
    secondary = Color(0xFFBDC8DC),
    onSecondary = Color(0xFF273141),
    secondaryContainer = Color(0xFF3D4758),
    onSecondaryContainer = Color(0xFFD9E3F3),
    tertiary = Color(0xFFFFB4A9),
    onTertiary = Color(0xFF561E18),
    tertiaryContainer = Color(0xFF73342D),
    onTertiaryContainer = Color(0xFFFFDAD4),
    background = Color(0xFF0B1320),
    onBackground = Color(0xFFE2E8F3),
    surface = Color(0xFF101A2A),
    onSurface = Color(0xFFE2E8F3),
    surfaceVariant = Color(0xFF424750),
    onSurfaceVariant = Color(0xFFC4C7CF),
    outline = Color(0xFF8E9199),
    error = Color(0xFFFFB4AB)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private fun blend(from: Color, to: Color, amount: Float): Color {
    val a = amount.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * a,
        green = from.green + (to.green - from.green) * a,
        blue = from.blue + (to.blue - from.blue) * a,
        alpha = 1f
    )
}

private fun onColor(background: Color): Color =
    if (background.luminance() > 0.46f) Color(0xFF111111) else Color.White

private fun withAccent(base: ColorScheme, accent: Color, dark: Boolean): ColorScheme {
    val primary = if (dark) blend(accent, Color.White, 0.22f) else accent
    val container = if (dark) blend(accent, Color.Black, 0.46f) else blend(accent, Color.White, 0.78f)
    val secondary = if (dark) blend(primary, Color(0xFFD5D5D5), 0.38f) else blend(primary, Color(0xFF5A5A5A), 0.42f)
    return base.copy(
        primary = primary,
        onPrimary = onColor(primary),
        primaryContainer = container,
        onPrimaryContainer = onColor(container),
        secondary = secondary,
        secondaryContainer = if (dark) blend(secondary, Color.Black, 0.5f) else blend(secondary, Color.White, 0.8f)
    )
}

@Composable
fun GrupoIdeaTheme(
    settings: PersonalizationSettings = PersonalizationSettings(),
    content: @Composable () -> Unit
) {
    val dark = when (settings.themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val accent = Color(colorInt(settings.accentColorHex, DEFAULT_ACCENT))
    val colors = withAccent(if (dark) DarkColors else LightColors, accent, dark)
    MaterialTheme(
        colorScheme = colors,
        shapes = AppShapes,
        content = content
    )
}
