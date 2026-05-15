package com.fluxsync.core.model

import kotlinx.serialization.Serializable

@Serializable
data class TransferFile(
    val transferId: String,
    val fileName: String,
    val relativePath: String,
    val sourcePath: String,
    val destinationPath: String,
    val totalSize: Long,
    val chunkSize: Int = 2_097_152,
    val totalChunks: Int,
    val xxhash: String,
    val state: TransferState = TransferState.Idle,
)
