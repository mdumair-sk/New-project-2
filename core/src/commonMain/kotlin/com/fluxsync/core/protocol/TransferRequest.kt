package com.fluxsync.core.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("TransferRequest")
data class TransferRequest(
    val transferId: String,
    val fileName: String,
    val totalSize: Long,
    val chunkSize: Int,
    val totalChunks: Int,
    val xxhash: String,
    val relativePath: String,
    val destinationPath: String = relativePath,
) : ControlMessage() {
    override val type: String = "TransferRequest"
}
