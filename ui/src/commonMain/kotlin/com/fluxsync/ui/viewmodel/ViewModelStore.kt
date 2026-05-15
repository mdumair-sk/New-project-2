package com.fluxsync.ui.viewmodel

data class ViewModelStore(
    val discoveryViewModel: DiscoveryViewModel,
    val pairingViewModel: PairingViewModel,
    val transferViewModel: TransferViewModel,
    val settingsViewModel: SettingsViewModel,
    val consoleViewModel: ConsoleViewModel,
)
