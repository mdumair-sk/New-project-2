package com.fluxsync.ui.screens

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fluxsync.ui.viewmodel.ViewModelStore

@Composable
fun AppRoot(navigator: AppNavigator, store: ViewModelStore) {
    val screen by navigator.currentScreen.collectAsState()
    Scaffold {
        when (screen) {
            Screen.Discovery -> DiscoveryScreen(store.discoveryViewModel, navigator)
            is Screen.Pairing -> PairingScreen(store.pairingViewModel, navigator)
            is Screen.ActiveTransfer -> ActiveTransferScreen(store.transferViewModel)
            Screen.Settings -> SettingsScreen(store.settingsViewModel)
            Screen.DevConsole -> DevConsoleScreen(store.consoleViewModel)
        }
    }
}
