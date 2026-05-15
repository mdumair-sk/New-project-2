package com.fluxsync.ui.platform

expect class VpnDetector() {
    fun isVpnActive(): Boolean
}
