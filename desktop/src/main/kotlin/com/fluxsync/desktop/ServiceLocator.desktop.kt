package com.fluxsync.desktop

import com.fluxsync.core.desktop.AdbEvent
import com.fluxsync.core.desktop.AdbPoller
import com.fluxsync.core.discovery.ManualEntryDiscovery
import com.fluxsync.core.discovery.PeerDiscovery
import com.fluxsync.core.discovery.PeerRegistry
import com.fluxsync.core.engine.AckTracker
import com.fluxsync.core.engine.ChunkDispatcher
import com.fluxsync.core.engine.PartFileScanner
import com.fluxsync.core.engine.TransferQueue
import com.fluxsync.core.logging.LogManager
import com.fluxsync.core.model.LinkType
import com.fluxsync.core.model.Peer
import com.fluxsync.core.model.PlatformSocket
import com.fluxsync.core.model.TransportLink
import com.fluxsync.core.network.DataChannelClient
import com.fluxsync.core.pairing.MonikerGenerator
import com.fluxsync.core.pairing.PairingSession
import com.fluxsync.core.pairing.TrustStore
import com.fluxsync.core.protocol.ProtocolConstants
import com.fluxsync.core.settings.SettingsKeys
import com.fluxsync.ui.platform.VpnDetector
import com.fluxsync.ui.screens.AppNavigator
import com.fluxsync.ui.viewmodel.ConsoleViewModel
import com.fluxsync.ui.viewmodel.DiscoveryViewModel
import com.fluxsync.ui.viewmodel.PairingViewModel
import com.fluxsync.ui.viewmodel.SettingsViewModel
import com.fluxsync.ui.viewmodel.TransferViewModel
import com.fluxsync.ui.viewmodel.ViewModelStore
import com.russhwolf.settings.PreferencesSettings
import java.util.prefs.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object ServiceLocator {
    val navigator = AppNavigator()
    private val settings = PreferencesSettings(Preferences.userRoot().node("com.fluxsync"))
    private val trustStore = TrustStore(settings)
    private val ackTracker = AckTracker()
    private val dataClient = DataChannelClient()
    private val transferQueue = TransferQueue()
    val adbPoller = AdbPoller()
    lateinit var viewModelStore: ViewModelStore
        private set

    fun init(scope: CoroutineScope) {
        val moniker = settings.getStringOrNull(SettingsKeys.LOCAL_MONIKER) ?: MonikerGenerator.generate().also {
            settings.putString(SettingsKeys.LOCAL_MONIKER, it)
        }
        val localPeer = Peer("desktop-local", moniker, false, "0.0.0.0", ProtocolConstants.WS_PORT)
        val registry = PeerRegistry(settings)
        val discovery = PeerDiscovery(scope)
        val pairing = PairingSession(scope)
        val dispatcher = ChunkDispatcher(scope, ackTracker.retryChannel) { _, payload -> dataClient.send(payload) }
        dispatcher.registerLink(TransportLink("wifi-local", LinkType.WIFI, PlatformSocket(), true))
        LogManager.init("${System.getProperty("user.home")}/.fluxsync/logs", scope)
        val settingsVm = SettingsViewModel(scope, settings, trustStore)
        val transferVm = TransferViewModel(
            scope = scope,
            ackTracker = ackTracker,
            dispatcher = dispatcher,
            transferQueue = transferQueue,
            scanner = PartFileScanner(),
            navigator = navigator,
            transfersDirectory = "${System.getProperty("user.home")}/.fluxsync/transfers",
            speedRefreshFast = settingsVm.speedRefreshFast,
            selectFiles = { DesktopFilePicker.selectFiles() },
            selectFolder = { DesktopFilePicker.selectFolder() },
        )
        viewModelStore = ViewModelStore(
            discoveryViewModel = DiscoveryViewModel(scope, registry, discovery, ManualEntryDiscovery(), VpnDetector(), navigator, localPeer),
            pairingViewModel = PairingViewModel(scope, pairing, trustStore, navigator),
            transferViewModel = transferVm,
            settingsViewModel = settingsVm,
            consoleViewModel = ConsoleViewModel(scope, LogManager.entries),
        )
        scope.launch {
            adbPoller.events.collect { event ->
                if (event is AdbEvent.AdbNotFound) transferVm.setAdbAvailable(false)
            }
        }
        adbPoller.start()
    }
}
