package com.fluxsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fluxsync.core.pairing.PairingState
import com.fluxsync.core.platform.currentTimeMillis
import com.fluxsync.ui.Strings
import com.fluxsync.ui.components.PinDisplay
import com.fluxsync.ui.theme.FluxColors
import com.fluxsync.ui.theme.FluxType
import com.fluxsync.ui.theme.Spacing
import com.fluxsync.ui.viewmodel.PairingViewModel
import kotlinx.coroutines.delay

@Composable
fun PairingScreen(viewModel: PairingViewModel, navigator: AppNavigator, modifier: Modifier = Modifier) {
    val state by viewModel.pairingState.collectAsState()
    var now by remember { mutableStateOf(currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = currentTimeMillis()
            delay(500)
        }
    }
    LaunchedEffect(state) {
        if (state is PairingState.Expired) navigator.back()
    }

    val awaiting = state as? PairingState.AwaitingConfirmation
    val pin = awaiting?.pin ?: "      "
    val seconds = awaiting?.let { ((it.expiresAt - now) / 1000L).coerceAtLeast(0L).toInt() } ?: 0

    Column(
        modifier = modifier.fillMaxSize().background(FluxColors.Background).padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(Strings.Pairing, color = FluxColors.OnBackground, style = FluxType.DisplaySmall)
        PinDisplay(pin = pin, expiresInSeconds = seconds, modifier = Modifier.padding(Spacing.xl))
        Button(enabled = awaiting != null, onClick = { viewModel.confirmPin(pin) }) {
            Text(Strings.Confirm)
        }
        if (state is PairingState.Expired) {
            Text(Strings.PinExpired, color = FluxColors.Error)
        }
    }
}
