package com.fluxsync.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FluxColorScheme = darkColorScheme(
    primary = FluxColors.Primary,
    onPrimary = FluxColors.OnPrimary,
    secondary = FluxColors.PrimaryVariant,
    background = FluxColors.Background,
    onBackground = FluxColors.OnBackground,
    surface = FluxColors.Surface,
    onSurface = FluxColors.OnSurface,
    surfaceVariant = FluxColors.SurfaceVariant,
    error = FluxColors.Error,
)

@Composable
fun FluxSyncTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FluxColorScheme,
        typography = FluxTypography,
        shapes = FluxShapes,
        content = content,
    )
}
