package com.fluxsync.ui.platform

import java.net.NetworkInterface

actual class VpnDetector actual constructor() {
    actual fun isVpnActive(): Boolean {
        return NetworkInterface.getNetworkInterfaces().asSequence().any { network ->
            val name = network.name.lowercase()
            network.isUp && (name.contains("tun") || name.contains("tap") || name.contains("ppp") || name.contains("vpn"))
        }
    }
}
