package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = RoyalBluePrimary,
    onPrimary = Color.White,
    primaryContainer = RoyalBlueContainer,
    onPrimaryContainer = OnRoyalBlueContainer,
    secondary = SlateSecondary,
    onSecondary = Color.White,
    secondaryContainer = SlateContainer,
    onSecondaryContainer = SlateSecondary,
    tertiary = SkyAccent,
    onTertiary = Color.White,
    tertiaryContainer = SkyContainer,
    onTertiaryContainer = SlateSecondary,
    background = LightBackground,
    onBackground = SlateSecondary,
    surface = LightSurface,
    onSurface = SlateSecondary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerContainerLight,
    onErrorContainer = DangerRed
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = Color(0xFF94A3B8),
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF38BDF8),
    onTertiary = DarkBackground,
    tertiaryContainer = Color(0xFF0C4A6E),
    onTertiaryContainer = Color(0xFFE0F2FE),
    background = DarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = Color(0xFFF87171),
    onError = DarkBackground,
    errorContainer = DangerContainerDark,
    onErrorContainer = Color(0xFFFECACA)
)

val TalentLensShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun TalentLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TalentLensTypography,
        shapes = TalentLensShapes,
        content = content
    )
}
