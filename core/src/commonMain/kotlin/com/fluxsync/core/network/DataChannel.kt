package com.fluxsync.core.network

import com.fluxsync.core.model.ChunkPayload
import com.fluxsync.core.protocol.ProtocolConstants
import kotlinx.coroutines.flow.SharedFlow

expect class DataChannelServer() {
    val incoming: SharedFlow<ChunkPayload>
    suspend fun start(port: Int = ProtocolConstants.DATA_PORT)
    suspend fun stop()
}

expect class DataChannelClient() {
    suspend fun connect(address: String, port: Int = ProtocolConstants.DATA_PORT)
    suspend fun send(payload: ChunkPayload)
    suspend fun close()
}
