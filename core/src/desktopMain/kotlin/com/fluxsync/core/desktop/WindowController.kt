package com.fluxsync.core.desktop

class WindowController(
    private val onExit: () -> Unit,
) {
    fun closeToTray() {
        SystemTrayManager.minimizeToTray()
    }

    fun exit() {
        SystemTrayManager.remove()
        onExit()
    }
}
