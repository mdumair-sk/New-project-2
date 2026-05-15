package com.fluxsync.core.discovery

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.fluxsync.core.model.Peer
import com.fluxsync.core.protocol.ProtocolConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual class PeerDiscovery actual constructor(
    private val scope: CoroutineScope,
) {
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    actual fun start(): Flow<DiscoveryResult> = callbackFlow {
        val context = AndroidDiscoveryEnvironment.context
        val manager = context?.getSystemService(NsdManager::class.java)
        if (manager == null) {
            trySend(DiscoveryResult.Searching)
            awaitClose { }
            return@callbackFlow
        }

        trySend(DiscoveryResult.Searching)
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != ProtocolConstants.MDNS_SERVICE_TYPE) return
                manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        val host = resolved.host?.hostAddress ?: return
                        val moniker = resolved.attributes["moniker"]?.decodeToString() ?: resolved.serviceName
                        trySend(
                            DiscoveryResult.Found(
                                Peer(
                                    id = resolved.serviceName,
                                    moniker = moniker,
                                    isTrusted = false,
                                    address = host,
                                    port = resolved.port,
                                ),
                            ),
                        )
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                trySend(DiscoveryResult.Lost(serviceInfo.serviceName))
            }

            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }
        discoveryListener = listener
        manager.discoverServices(ProtocolConstants.MDNS_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose {
            runCatching { manager.stopServiceDiscovery(listener) }
            discoveryListener = null
        }
    }

    actual suspend fun advertise(peer: Peer) {
        val context = AndroidDiscoveryEnvironment.context ?: return
        val manager = context.getSystemService(NsdManager::class.java)
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = peer.id
            serviceType = ProtocolConstants.MDNS_SERVICE_TYPE
            port = peer.port
            setAttribute("moniker", peer.moniker)
        }
        registrationListener?.let { runCatching { manager.unregisterService(it) } }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
        manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    actual suspend fun stop() {
        val context = AndroidDiscoveryEnvironment.context ?: return
        val manager = context.getSystemService(NsdManager::class.java)
        registrationListener?.let { runCatching { manager.unregisterService(it) } }
        discoveryListener?.let { runCatching { manager.stopServiceDiscovery(it) } }
        registrationListener = null
        discoveryListener = null
    }
}
