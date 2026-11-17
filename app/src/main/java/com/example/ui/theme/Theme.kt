package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NightShiftColorScheme = darkColorScheme(
    primary = FlashlightAmber,
    onPrimary = VoidObsidian,
    primaryContainer = ElevatedPlum,
    onPrimaryContainer = WarmTungsten,
    secondary = ArcadeCyan,
    onSecondary = VoidObsidian,
    secondaryContainer = MidnightCharcoal,
    onSecondaryContainer = ArcadeCyan,
    tertiary = PhosphorGreen,
    onTertiary = VoidObsidian,
    error = CrimsonAlert,
    onError = BoneWhite,
    errorContainer = DeepVelvetRed,
    onErrorContainer = BoneWhite,
    background = VoidObsidian,
    onBackground = BoneWhite,
    surface = MidnightCharcoal,
    onSurface = BoneWhite,
    surfaceVariant = ElevatedPlum,
    onSurfaceVariant = MutedAsh,
    outline = BorderSlate
)

private val NightShiftShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun NightShiftTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NightShiftColorScheme,
        typography = Typography,
        shapes = NightShiftShapes,
        content = content
    )
}
