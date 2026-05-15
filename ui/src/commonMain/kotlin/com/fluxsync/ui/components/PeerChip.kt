package com.fluxsync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.fluxsync.core.model.Peer
import com.fluxsync.ui.Strings
import com.fluxsync.ui.theme.FluxColors
import com.fluxsync.ui.theme.FluxShape
import com.fluxsync.ui.theme.FluxType
import com.fluxsync.ui.theme.Spacing

@Composable
fun PeerChip(peer: Peer, isConnected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(FluxShape.Chip)
            .background(FluxColors.SurfaceVariant)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(Spacing.md)
                .clip(CircleShape)
                .background(if (isConnected) FluxColors.Success else FluxColors.Divider),
        )
        Text(text = peer.moniker, color = FluxColors.OnBackground, style = FluxType.BodyMedium)
        if (peer.isTrusted) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = Strings.Lock,
                tint = FluxColors.Primary,
                modifier = Modifier.size(Spacing.md),
            )
        } else {
            Box(modifier = Modifier.width(Spacing.xs))
        }
    }
}
