package com.fluxsync.ui.viewmodel

import com.fluxsync.core.discovery.DiscoveryResult
import com.fluxsync.core.discovery.ManualEntryDiscovery
import com.fluxsync.core.discovery.PeerDiscovery
import com.fluxsync.core.discovery.PeerRegistry
import com.fluxsync.core.model.Peer
import com.fluxsync.ui.platform.VpnDetector
import com.fluxsync.ui.screens.AppNavigator
import com.fluxsync.ui.screens.Screen
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DiscoveryViewModel(
    scope: kotlinx.coroutines.CoroutineScope,
    private val peerRegistry: PeerRegistry,
    private val peerDiscovery: PeerDiscovery,
    private val manualEntryDiscovery: ManualEntryDiscovery,
    private val vpnDetector: VpnDetector,
    private val navigator: AppNavigator,
    private val localPeer: Peer,
) : FluxViewModel(scope) {
    val peers: StateFlow<List<Peer>> = peerRegistry.getAll()
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching
    private val _vpnDetected = MutableStateFlow(false)
    val vpnDetected: StateFlow<Boolean> = _vpnDetected
    private var discoveryJob: Job? = null

    fun startDiscovery() = launchLogged("DISCOVERY") {
        _vpnDetected.value = vpnDetector.isVpnActive()
        _isSearching.value = true
        peerDiscovery.advertise(localPeer)
        discoveryJob?.cancel()
        discoveryJob = scope.launch {
            peerDiscovery.start().collect { result ->
                when (result) {
                    is DiscoveryResult.Found -> peerRegistry.addOrUpdate(result.peer)
                    is DiscoveryResult.Lost -> peerRegistry.remove(result.peerId)
                    DiscoveryResult.Searching -> _isSearching.value = true
                }
            }
        }
    }

    fun stopDiscovery() = launchLogged("DISCOVERY") {
        discoveryJob?.cancel()
        peerDiscovery.stop()
        _isSearching.value = false
    }

    fun connectManual(input: String) = launchLogged("DISCOVERY") {
        val peer = manualEntryDiscovery.resolve(input, localPeer).getOrThrow()
        peerRegistry.addOrUpdate(peer)
        navigator.navigate(Screen.Pairing(peer.id))
    }

    fun openPairing(peer: Peer) {
        navigator.navigate(Screen.Pairing(peer.id))
    }
}
