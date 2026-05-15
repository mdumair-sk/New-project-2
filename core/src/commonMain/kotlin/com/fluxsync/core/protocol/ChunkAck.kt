package com.fluxsync.core.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ChunkACK")
data class ChunkAck(
    val transferId: String,
    val sequenceId: Int,
) : ControlMessage() {
    override val type: String = "ChunkACK"
}
