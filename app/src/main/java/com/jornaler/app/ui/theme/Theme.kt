package com.jornaler.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jornaler.app.data.ThemeMode

private val Ink = Color(0xFF2B2118)
private val InkSoft = Color(0xFF5C4E42)
private val Paper = Color(0xFFF6EFE4)
private val PaperBright = Color(0xFFFFFBF6)
private val PaperDeep = Color(0xFFEFE2D2)
private val Terracotta = Color(0xFF8C4A32)
private val TerracottaDeep = Color(0xFF5C2E1C)
private val Clay = Color(0xFFF0C7B0)
private val Sage = Color(0xFF3E6244)
private val SageMist = Color(0xFFD5E6D2)

private val NightInk = Color(0xFFF3E7DA)
private val NightMuted = Color(0xFFD2C0B0)
private val NightPaper = Color(0xFF1C1714)
private val NightSurface = Color(0xFF2A2320)
private val NightSurfaceHigh = Color(0xFF3A312C)
private val NightTerracotta = Color(0xFFE7B9A4)
private val NightSage = Color(0xFFB7D0AE)

val LightColors = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color(0xFFFFF8F3),
    primaryContainer = Clay,
    onPrimaryContainer = TerracottaDeep,
    secondary = Sage,
    onSecondary = Color(0xFFF4FFF1),
    secondaryContainer = SageMist,
    onSecondaryContainer = Color(0xFF16301C),
    tertiary = Color(0xFF8A5A44),
    onTertiary = Color(0xFFFFF8F3),
    tertiaryContainer = Color(0xFFF8E0D2),
    onTertiaryContainer = Color(0xFF3A2418),
    background = Paper,
    onBackground = Ink,
    surface = PaperBright,
    onSurface = Ink,
    surfaceVariant = PaperDeep,
    onSurfaceVariant = InkSoft,
    surfaceContainerLowest = Color(0xFFFFFDF9),
    surfaceContainerLow = Color(0xFFFBF4EA),
    surfaceContainer = Color(0xFFF3E7D8),
    surfaceContainerHigh = Color(0xFFEADCCB),
    outline = Color(0xFFD5C4B0),
    outlineVariant = Color(0xFFE6D5C4),
    error = Color(0xFF9C3B32),
    onError = Color(0xFFFFF7F6),
)

val DarkColors = darkColorScheme(
    primary = NightTerracotta,
    onPrimary = Color(0xFF3F2414),
    primaryContainer = Color(0xFF6B3E28),
    onPrimaryContainer = Color(0xFFFFDCC6),
    secondary = NightSage,
    onSecondary = Color(0xFF1C3320),
    secondaryContainer = Color(0xFF354A32),
    onSecondaryContainer = Color(0xFFD3ECC9),
    tertiary = Color(0xFFE8C0A8),
    onTertiary = Color(0xFF3A2418),
    tertiaryContainer = Color(0xFF5A4034),
    onTertiaryContainer = Color(0xFFF8E0D2),
    background = NightPaper,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightMuted,
    surfaceContainerLowest = Color(0xFF161210),
    surfaceContainerLow = Color(0xFF241C18),
    surfaceContainer = Color(0xFF2E2622),
    surfaceContainerHigh = Color(0xFF3C322C),
    outline = Color(0xFF6E5E52),
    outlineVariant = Color(0xFF4A3E36),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

val JornalerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun JornalerTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = JornalerTypography,
        shapes = JornalerShapes,
        content = content,
    )
}
