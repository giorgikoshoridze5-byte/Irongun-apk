package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val IronfrontColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = ObsidianBg,
    primaryContainer = AmberMuted,
    onPrimaryContainer = AmberPrimary,
    secondary = CyanAllied,
    onSecondary = ObsidianBg,
    secondaryContainer = CyanMuted,
    onSecondaryContainer = CyanAllied,
    tertiary = CrimsonThreat,
    onTertiary = TextPrimary,
    tertiaryContainer = CrimsonMuted,
    onTertiaryContainer = CrimsonThreat,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = GunmetalSurface,
    onSurface = TextPrimary,
    surfaceVariant = TacticalSlate,
    onSurfaceVariant = TextSecondary,
    outline = SteelBorder,
    error = CrimsonThreat,
    onError = TextPrimary
)

val IronfrontShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun IronfrontTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IronfrontColorScheme,
        typography = IronfrontTypography,
        shapes = IronfrontShapes,
        content = content
    )
}
