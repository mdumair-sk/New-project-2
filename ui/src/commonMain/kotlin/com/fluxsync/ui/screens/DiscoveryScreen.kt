package com.fluxsync.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.fluxsync.ui.Strings
import com.fluxsync.ui.components.PeerChip
import com.fluxsync.ui.theme.FluxColors
import com.fluxsync.ui.theme.FluxType
import com.fluxsync.ui.theme.Spacing
import com.fluxsync.ui.viewmodel.DiscoveryViewModel

@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
) {
    val peers by viewModel.peers.collectAsState()
    val vpnDetected by viewModel.vpnDetected.collectAsState()
    var showManual by remember { mutableStateOf(false) }
    var manualInput by remember { mutableStateOf("") }
    val pulse by rememberInfiniteTransition(label = "search")
        .animateFloat(0.45f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "searchAlpha")

    LaunchedEffect(Unit) { viewModel.startDiscovery() }

    Box(modifier = modifier.fillMaxSize().background(FluxColors.Background).padding(Spacing.lg)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.Discovery, color = FluxColors.OnBackground, style = FluxType.DisplaySmall)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { navigator.navigate(Screen.DevConsole) }) {
                    Icon(Icons.Default.Info, contentDescription = Strings.OpenConsole, tint = FluxColors.OnSurface)
                }
                IconButton(onClick = { navigator.navigate(Screen.Settings) }) {
                    Icon(Icons.Default.Settings, contentDescription = Strings.OpenSettings, tint = FluxColors.OnSurface)
                }
            }
            if (vpnDetected) {
                Text(Strings.VpnWarning, color = FluxColors.Warning, style = FluxType.BodyMedium)
            }
            if (peers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(Strings.NoDevices, color = FluxColors.Primary, style = FluxType.TitleLarge, modifier = Modifier.alpha(pulse))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(peers) { peer ->
                        PeerChip(peer = peer, isConnected = true, onClick = { viewModel.openPairing(peer) }, modifier = Modifier.padding(Spacing.sm))
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showManual = true },
            modifier = Modifier.align(Alignment.BottomEnd),
            containerColor = FluxColors.Primary,
            contentColor = FluxColors.OnPrimary,
        ) {
            Icon(Icons.Default.Add, contentDescription = Strings.AddManualPeer)
        }
    }

    if (showManual) {
        AlertDialog(
            onDismissRequest = { showManual = false },
            title = { Text(Strings.ManualConnect) },
            text = {
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    label = { Text(Strings.EnterIp) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.connectManual(manualInput)
                    showManual = false
                }) { Text(Strings.Connect) }
            },
            dismissButton = {
                TextButton(onClick = { showManual = false }) { Text(Strings.Cancel) }
            },
        )
    }
}
