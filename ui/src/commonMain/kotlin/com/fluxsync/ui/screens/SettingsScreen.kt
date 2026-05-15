package com.fluxsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fluxsync.core.protocol.ProtocolConstants
import com.fluxsync.ui.Strings
import com.fluxsync.ui.components.formatBytes
import com.fluxsync.ui.theme.FluxColors
import com.fluxsync.ui.theme.FluxType
import com.fluxsync.ui.theme.Spacing
import com.fluxsync.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val chunkSize by viewModel.chunkSizeBytes.collectAsState()
    val fast by viewModel.speedRefreshFast.collectAsState()
    val trusted by viewModel.trustedDevices.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().background(FluxColors.Background).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(Strings.Settings, color = FluxColors.OnBackground, style = FluxType.DisplaySmall)
        Text("${Strings.ChunkSize}: ${formatBytes(chunkSize.toLong())}", color = FluxColors.OnBackground)
        Slider(
            value = chunkSize.toFloat(),
            onValueChange = { viewModel.setChunkSize(it.toInt()) },
            valueRange = ProtocolConstants.MIN_CHUNK_SIZE.toFloat()..ProtocolConstants.MAX_CHUNK_SIZE.toFloat(),
            steps = 4,
        )
        Text(Strings.RefreshRate, color = FluxColors.OnBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilterChip(selected = fast, onClick = { if (!fast) viewModel.toggleSpeedRefresh() }, label = { Text(Strings.FastRefresh) })
            FilterChip(selected = !fast, onClick = { if (fast) viewModel.toggleSpeedRefresh() }, label = { Text(Strings.AverageRefresh) })
        }
        Text(Strings.TrustedDevices, color = FluxColors.OnBackground, style = FluxType.TitleLarge)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.weight(1f)) {
            items(trusted) { record ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(record.moniker, color = FluxColors.OnSurface, modifier = Modifier.weight(1f))
                    Button(onClick = { viewModel.removeTrustedDevice(record.peerId) }) { Text(Strings.Remove) }
                }
            }
        }
        Text(Strings.AppVersion, color = FluxColors.OnSurface, style = FluxType.BodySmall)
    }
}
