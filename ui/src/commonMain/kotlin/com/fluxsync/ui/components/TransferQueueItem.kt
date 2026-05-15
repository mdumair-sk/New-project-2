package com.fluxsync.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fluxsync.core.model.TransferFile
import com.fluxsync.core.model.TransferState
import com.fluxsync.ui.Strings
import com.fluxsync.ui.theme.FluxColors
import com.fluxsync.ui.theme.FluxType
import com.fluxsync.ui.theme.Spacing

@Composable
fun TransferQueueItem(
    file: TransferFile,
    state: TransferState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(text = file.fileName, color = FluxColors.OnBackground, style = FluxType.TitleMedium)
                Text(
                    text = if (file.totalSize > 0) formatBytes(file.totalSize) else Strings.UnknownSize,
                    color = FluxColors.OnSurface,
                    style = FluxType.BodySmall,
                )
            }
            if (state is TransferState.Transferring || state is TransferState.Negotiating) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = Strings.Cancel, tint = FluxColors.Error)
                }
            }
        }
        Text(text = state.label(), color = FluxColors.OnSurface, style = FluxType.BodySmall)
        if (state is TransferState.Transferring) {
            val progress = if (state.totalChunks == 0) 0f else state.receivedChunks.size.toFloat() / state.totalChunks.toFloat()
            FluxProgressBar(progress = progress)
        }
    }
}

private fun TransferState.label(): String = when (this) {
    TransferState.Idle -> Strings.StateIdle
    TransferState.Negotiating -> Strings.StateNegotiating
    is TransferState.Transferring -> Strings.StateTransferring
    is TransferState.PausedReconnect -> Strings.StateReconnect
    TransferState.Verifying -> Strings.StateVerifying
    TransferState.Finalizing -> Strings.StateFinalizing
    TransferState.Complete -> Strings.StateComplete
    is TransferState.Aborted -> Strings.StateAborted
}
