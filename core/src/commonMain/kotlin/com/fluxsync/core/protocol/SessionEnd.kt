package com.fluxsync.core.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("SessionEnd")
data class SessionEnd(
    val transferId: String,
    val reason: SessionEndReason,
    val message: String? = null,
) : ControlMessage() {
    override val type: String = "SessionEnd"
}

@Serializable
enum class SessionEndReason {
    COMPLETE,
    CANCELLED,
    ERROR,
}
