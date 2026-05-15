package com.fluxsync.core.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("NACK")
data class Nack(
    val transferId: String,
    val sequenceId: Int,
    val reason: NackReason,
) : ControlMessage() {
    override val type: String = "NACK"
}

@Serializable
enum class NackReason {
    DISK_WRITE_FAILURE,
    CHECKSUM_MISMATCH,
}
