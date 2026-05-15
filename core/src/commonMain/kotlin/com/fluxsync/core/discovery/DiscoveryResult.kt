package com.fluxsync.core.discovery

import com.fluxsync.core.model.Peer

sealed class DiscoveryResult {
    data class Found(val peer: Peer) : DiscoveryResult()
    data class Lost(val peerId: String) : DiscoveryResult()
    data object Searching : DiscoveryResult()
}
