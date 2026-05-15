package com.fluxsync.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.fluxsync.ui.theme.FluxColors
import com.fluxsync.ui.theme.FluxType

@Composable
fun SpeedGauge(bytesPerSecond: Long, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(bytesPerSecond.toFloat(), label = "speed")
    val color: Color = if (bytesPerSecond > 0) FluxColors.SpeedGreen else FluxColors.OnSurface
    Column(modifier = modifier) {
        Text(
            text = formatSpeed(animated.toLong()),
            color = color,
            style = FluxType.DisplaySmall.merge(FluxType.LabelMono),
        )
    }
}
