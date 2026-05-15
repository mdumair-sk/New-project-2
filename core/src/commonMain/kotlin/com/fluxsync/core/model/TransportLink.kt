package com.fluxsync.core.model

data class TransportLink(
    val id: String,
    val type: LinkType,
    val socket: PlatformSocket,
    val isActive: Boolean,
)

enum class LinkType {
    ADB,
    WIFI,
}
