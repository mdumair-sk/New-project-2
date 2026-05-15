package com.fluxsync.core.desktop

import java.awt.Color
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

object SystemTrayManager {
    private var trayIcon: TrayIcon? = null
    private var onOpen: (() -> Unit)? = null
    private var onQuit: (() -> Unit)? = null

    fun install(onOpen: () -> Unit, onQuit: () -> Unit) {
        this.onOpen = onOpen
        this.onQuit = onQuit
        if (!SystemTray.isSupported() || trayIcon != null) return
        val menu = PopupMenu()
        menu.add(MenuItem("Open FluxSync").apply { addActionListener { onOpen() } })
        menu.add(MenuItem("Quit").apply { addActionListener { onQuit() } })
        trayIcon = TrayIcon(createIcon(), "FluxSync", menu).apply {
            isImageAutoSize = true
            addActionListener { onOpen() }
        }
    }

    fun minimizeToTray() {
        val icon = trayIcon ?: return
        runCatching { SystemTray.getSystemTray().add(icon) }
    }

    fun remove() {
        trayIcon?.let { SystemTray.getSystemTray().remove(it) }
    }

    private fun createIcon(): BufferedImage {
        val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.color = Color(13, 13, 13)
        graphics.fillRect(0, 0, 32, 32)
        graphics.color = Color(79, 195, 247)
        graphics.fillOval(5, 5, 22, 22)
        graphics.dispose()
        return image
    }
}
