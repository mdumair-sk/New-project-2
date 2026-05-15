package com.fluxsync.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.fluxsync.ui.theme.FluxColors
import com.fluxsync.ui.theme.FluxType
import com.fluxsync.ui.theme.Spacing

@Composable
fun FluxProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawTrack(animated)
        }
        Text(
            text = "${(animated * 100).toInt()}%",
            color = FluxColors.OnBackground,
            style = FluxType.LabelMono,
        )
    }
}

private fun DrawScope.drawTrack(progress: Float) {
    val radius = CornerRadius(Spacing.sm.toPx(), Spacing.sm.toPx())
    drawRoundRect(color = FluxColors.SurfaceVariant, size = size, cornerRadius = radius)
    drawRoundRect(
        color = FluxColors.Primary,
        size = Size(width = size.width * progress, height = size.height),
        cornerRadius = radius,
    )
}
