package com.fluxsync.core.network

import com.fluxsync.core.protocol.ControlMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ControlChannel {
    suspend fun connect(address: String, port: Int)
    suspend fun send(message: ControlMessage)
    fun incoming(): Flow<ControlMessage>
    suspend fun disconnect()
    val isConnected: StateFlow<Boolean>
}
