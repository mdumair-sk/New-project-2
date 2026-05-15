package com.fluxsync.core.network

import com.fluxsync.core.model.ChunkPayload
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

actual class DataChannelServer actual constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _incoming = MutableSharedFlow<ChunkPayload>(replay = 1, extraBufferCapacity = 128)
    private var server: ServerSocket? = null

    actual val incoming: SharedFlow<ChunkPayload> = _incoming

    actual suspend fun start(port: Int) {
        if (server != null) return
        server = ServerSocket(port)
        scope.launch {
            val activeServer = server ?: return@launch
            while (!activeServer.isClosed) {
                runCatching {
                    val socket = activeServer.accept()
                    launch { readSocket(socket) }
                }
            }
        }
    }

    actual suspend fun stop() {
        server?.close()
        server = null
        scope.cancel()
    }

    private suspend fun readSocket(socket: Socket) {
        socket.use {
            val input = DataInputStream(it.getInputStream())
            while (!it.isClosed) {
                val idLength = input.readInt()
                val idBytes = ByteArray(idLength)
                input.readFully(idBytes)
                val sequenceId = input.readInt()
                val dataLength = input.readInt()
                val data = ByteArray(dataLength)
                input.readFully(data)
                _incoming.emit(ChunkPayload(idBytes.decodeToString(), sequenceId, data))
            }
        }
    }
}

actual class DataChannelClient actual constructor() {
    private var socket: Socket? = null
    private var output: DataOutputStream? = null

    actual suspend fun connect(address: String, port: Int) {
        val connected = Socket(address, port)
        socket = connected
        output = DataOutputStream(connected.getOutputStream())
    }

    actual suspend fun send(payload: ChunkPayload) {
        val out = output ?: error("Data channel is not connected")
        val idBytes = payload.transferId.encodeToByteArray()
        out.writeInt(idBytes.size)
        out.write(idBytes)
        out.writeInt(payload.sequenceId)
        out.writeInt(payload.data.size)
        out.write(payload.data)
        out.flush()
    }

    actual suspend fun close() {
        output?.close()
        output = null
        socket?.close()
        socket = null
    }
}
