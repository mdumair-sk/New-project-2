package com.fluxsync.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PartFileMeta(
    val transferId: String,
    val fileName: String,
    val totalChunks: Int,
    val chunkSize: Int,
    val xxhash: String,
    val receivedChunks: List<Int>,
)
