package com.fluxsync.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object FluxShape {
    val Card = RoundedCornerShape(8.dp)
    val Button = RoundedCornerShape(8.dp)
    val Chip = RoundedCornerShape(100.dp)
}

val FluxShapes = Shapes(
    small = FluxShape.Button,
    medium = FluxShape.Card,
    large = FluxShape.Card,
)
