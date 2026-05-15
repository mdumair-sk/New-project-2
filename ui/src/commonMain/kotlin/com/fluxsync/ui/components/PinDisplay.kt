package com.fluxsync.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.fluxsync.ui.theme.FluxColors
import com.fluxsync.ui.theme.FluxShape
import com.fluxsync.ui.theme.FluxType
import com.fluxsync.ui.theme.Spacing

@Composable
fun PinDisplay(pin: String, expiresInSeconds: Int, modifier: Modifier = Modifier) {
    val progress = (expiresInSeconds / 60f).coerceIn(0f, 1f)
    val ringColor = if (expiresInSeconds <= 10) FluxColors.Error else FluxColors.Primary
    Box(modifier = modifier.size(Spacing.xxl * 4), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawArc(
                color = FluxColors.SurfaceVariant,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = Spacing.xs.toPx(), cap = StrokeCap.Round),
                size = Size(size.width, size.height),
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = Spacing.xs.toPx(), cap = StrokeCap.Round),
                size = Size(size.width, size.height),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            pin.padEnd(6, ' ').take(6).forEach { digit ->
                Box(
                    modifier = Modifier
                        .background(FluxColors.SurfaceVariant, FluxShape.Card)
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = digit.toString(), color = FluxColors.OnBackground, style = FluxType.DisplaySmall)
                }
            }
        }
    }
}
