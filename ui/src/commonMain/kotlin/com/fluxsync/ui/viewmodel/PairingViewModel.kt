package com.fluxsync.ui.viewmodel

import com.fluxsync.core.model.Peer
import com.fluxsync.core.pairing.PairingSession
import com.fluxsync.core.pairing.PairingState
import com.fluxsync.core.pairing.TrustStore
import com.fluxsync.core.pairing.TrustedRecord
import com.fluxsync.core.pairing.toHexString
import com.fluxsync.ui.screens.AppNavigator
import com.fluxsync.ui.screens.Screen
import kotlinx.coroutines.flow.StateFlow

class PairingViewModel(
    scope: kotlinx.coroutines.CoroutineScope,
    private val pairingSession: PairingSession,
    private val trustStore: TrustStore,
    private val navigator: AppNavigator,
) : FluxViewModel(scope) {
    val pairingState: StateFlow<PairingState> = pairingSession.state
    private var pendingPeer: Peer? = null

    fun initiatePairing(peer: Peer) = launchLogged("PAIRING") {
        pendingPeer = peer
        pairingSession.start()
    }

    fun confirmPin(input: String) = launchLogged("PAIRING") {
        if (!pairingSession.confirmPin(input)) return@launchLogged
        val peer = pendingPeer ?: return@launchLogged
        val confirmed = pairingState.value as? PairingState.Confirmed ?: return@launchLogged
        trustStore.save(
            TrustedRecord(
                peerId = peer.id,
                moniker = peer.moniker,
                keyHex = confirmed.encryptedKey.toHexString(),
                saltHex = confirmed.salt.toHexString(),
            ),
        )
        confirmed.encryptedKey.fill(0)
        navigator.navigate(Screen.ActiveTransfer(peer.id))
    }
}
