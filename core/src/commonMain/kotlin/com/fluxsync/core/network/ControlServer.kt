package com.fluxsync.core.network

import com.fluxsync.core.protocol.ControlMessage
import com.fluxsync.core.protocol.ProtocolConstants
import kotlinx.coroutines.flow.SharedFlow

expect class ControlServer() {
    val incoming: SharedFlow<ControlMessage>
    suspend fun start(port: Int = ProtocolConstants.WS_PORT)
    suspend fun send(message: ControlMessage)
    suspend fun stop()
}
