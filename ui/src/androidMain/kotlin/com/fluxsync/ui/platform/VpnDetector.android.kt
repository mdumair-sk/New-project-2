package com.fluxsync.ui.platform

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.fluxsync.core.discovery.AndroidDiscoveryEnvironment

actual class VpnDetector actual constructor() {
    actual fun isVpnActive(): Boolean {
        val context = AndroidDiscoveryEnvironment.context ?: return false
        val manager = context.getSystemService(ConnectivityManager::class.java)
        return manager.allNetworks.any { network ->
            manager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }
    }
}
