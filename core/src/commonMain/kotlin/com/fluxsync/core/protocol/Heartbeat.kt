package com.fluxsync.core.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Heartbeat")
data class Heartbeat(
    val timestamp: Long,
) : ControlMessage() {
    override val type: String = "Heartbeat"
}
