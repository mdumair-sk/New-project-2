package com.fluxsync.core.discovery

import com.fluxsync.core.model.Peer
import com.fluxsync.core.protocol.ProtocolConstants
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual class PeerDiscovery actual constructor(
    private val scope: CoroutineScope,
) {
    private var jmdns: JmDNS? = null
    private var serviceInfo: ServiceInfo? = null

    actual fun start(): Flow<DiscoveryResult> = callbackFlow {
        val dns = ensureJmDns()
        trySend(DiscoveryResult.Searching)
        val listener = object : ServiceListener {
            override fun serviceAdded(event: ServiceEvent) {
                dns.requestServiceInfo(event.type, event.name, true)
            }

            override fun serviceRemoved(event: ServiceEvent) {
                trySend(DiscoveryResult.Lost(event.name))
            }

            override fun serviceResolved(event: ServiceEvent) {
                val info = event.info
                val address = info.inet4Addresses.firstOrNull()?.hostAddress ?: info.hostAddresses.firstOrNull() ?: return
                trySend(
                    DiscoveryResult.Found(
                        Peer(
                            id = event.name,
                            moniker = info.getPropertyString("moniker") ?: event.name,
                            isTrusted = false,
                            address = address,
                            port = info.port,
                        ),
                    ),
                )
            }
        }
        dns.addServiceListener(ProtocolConstants.MDNS_SERVICE_TYPE, listener)
        awaitClose { dns.removeServiceListener(ProtocolConstants.MDNS_SERVICE_TYPE, listener) }
    }

    actual suspend fun advertise(peer: Peer) {
        val dns = ensureJmDns()
        serviceInfo?.let { dns.unregisterService(it) }
        serviceInfo = ServiceInfo.create(
            ProtocolConstants.MDNS_SERVICE_TYPE,
            peer.id,
            peer.port,
            0,
            0,
            mapOf("moniker" to peer.moniker),
        )
        dns.registerService(serviceInfo)
    }

    actual suspend fun stop() {
        serviceInfo?.let { jmdns?.unregisterService(it) }
        serviceInfo = null
        jmdns?.unregisterAllServices()
        jmdns?.close()
        jmdns = null
    }

    private fun ensureJmDns(): JmDNS {
        val existing = jmdns
        if (existing != null) return existing
        return JmDNS.create(InetAddress.getLocalHost()).also { jmdns = it }
    }
}
