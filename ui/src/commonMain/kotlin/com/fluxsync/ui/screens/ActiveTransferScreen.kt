package com.fluxsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fluxsync.ui.Strings
import com.fluxsync.ui.components.AdbWarningBanner
import com.fluxsync.ui.components.ConsoleLog
import com.fluxsync.ui.components.FluxProgressBar
import com.fluxsync.ui.components.StorageBanner
import com.fluxsync.ui.components.TransferQueueItem
import com.fluxsync.ui.components.formatEta
import com.fluxsync.ui.components.SpeedGauge
import com.fluxsync.ui.theme.FluxColors
import com.fluxsync.ui.theme.FluxType
import com.fluxsync.ui.theme.Spacing
import com.fluxsync.ui.viewmodel.TransferViewModel

@Composable
fun ActiveTransferScreen(viewModel: TransferViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.transferState.collectAsState()
    val speed by viewModel.speedBytesPerSec.collectAsState()
    val eta by viewModel.etaSeconds.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val partBytes by viewModel.partFilesBytes.collectAsState()
    val adbAvailable by viewModel.adbAvailable.collectAsState()
    val logs by viewModel.logEntries.collectAsState()
    var showDetails by remember { mutableStateOf(false) }
    var adbDismissed by remember { mutableStateOf(false) }

    val progress = queue.firstOrNull()?.let { file ->
        val transferring = state as? com.fluxsync.core.model.TransferState.Transferring
        if (transferring == null || file.totalChunks == 0) 0f else transferring.receivedChunks.size.toFloat() / file.totalChunks.toFloat()
    } ?: 0f

    Column(
        modifier = modifier.fillMaxSize().background(FluxColors.Background).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(Strings.ActiveTransfer, color = FluxColors.OnBackground, style = FluxType.DisplaySmall)
        if (!adbAvailable && !adbDismissed) {
            AdbWarningBanner(onDismiss = { adbDismissed = true })
        }
        StorageBanner(partFilesBytes = partBytes, onEmptyTrash = viewModel::emptyTrash)
        SpeedGauge(bytesPerSecond = speed)
        FluxProgressBar(progress = progress)
        Text("${formatEta(eta)} ${Strings.Remaining}", color = FluxColors.OnSurface, style = FluxType.BodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(onClick = viewModel::requestAddFiles) {
                Icon(Icons.Default.InsertDriveFile, contentDescription = Strings.Files)
                Text(Strings.AddFiles)
            }
            OutlinedButton(onClick = viewModel::requestAddFolder) {
                Icon(Icons.Default.Folder, contentDescription = Strings.Folder)
                Text(Strings.AddFolder)
            }
            OutlinedButton(onClick = { showDetails = !showDetails }) {
                Text(if (showDetails) Strings.HideDetails else Strings.ShowDetails)
            }
        }
        Text(Strings.Waitlist, color = FluxColors.OnBackground, style = FluxType.TitleLarge)
        if (queue.isEmpty()) {
            Text(Strings.EmptyQueue, color = FluxColors.OnSurface)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.weight(1f)) {
                items(queue) { file ->
                    TransferQueueItem(file = file, state = file.state, onCancel = viewModel::cancelCurrent)
                }
            }
        }
        if (showDetails) {
            ConsoleLog(entries = logs, modifier = Modifier.fillMaxWidth().heightIn(max = Spacing.xxl * 5))
        }
    }
}
