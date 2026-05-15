package com.fluxsync.core.discovery

import com.fluxsync.core.model.Peer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

expect class PeerDiscovery(scope: CoroutineScope) {
    fun start(): Flow<DiscoveryResult>
    suspend fun advertise(peer: Peer)
    suspend fun stop()
}
