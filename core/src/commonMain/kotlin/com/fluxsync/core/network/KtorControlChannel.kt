package com.fluxsync.core.network

import com.fluxsync.core.platform.currentTimeMillis
import com.fluxsync.core.protocol.ControlJson
import com.fluxsync.core.protocol.ControlMessage
import com.fluxsync.core.protocol.Heartbeat
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

class KtorControlChannel(
    private val scope: CoroutineScope,
    private val client: HttpClient = HttpClient { install(WebSockets) },
) : ControlChannel {
    private val _incoming = MutableSharedFlow<ControlMessage>(replay = 1, extraBufferCapacity = 64)
    private val _isConnected = MutableStateFlow(false)
    private var session: WebSocketSession? = null
    private var socketJob: Job? = null
    private var heartbeatJob: Job? = null

    override val isConnected: StateFlow<Boolean> = _isConnected

    override suspend fun connect(address: String, port: Int) {
        socketJob?.cancel()
        socketJob = scope.launch {
            runCatching {
                client.webSocket(method = HttpMethod.Get, host = address, port = port, path = "/control") {
                    session = this
                    _isConnected.value = true
                    startHeartbeat()
                    for (frame in incoming) {
                        val text = (frame as? Frame.Text)?.readText() ?: continue
                        val message = ControlJson.instance.decodeFromString(ControlMessage.serializer(), text)
                        _incoming.emit(message)
                    }
                }
            }.onFailure {
                _isConnected.value = false
            }
            _isConnected.value = false
            heartbeatJob?.cancel()
            session = null
        }
    }

    override suspend fun send(message: ControlMessage) {
        val active = session ?: error("Control channel is not connected")
        val raw = ControlJson.instance.encodeToString(ControlMessage.serializer(), message)
        active.send(Frame.Text(raw))
    }

    override fun incoming(): Flow<ControlMessage> = _incoming

    override suspend fun disconnect() {
        heartbeatJob?.cancel()
        socketJob?.cancel()
        session?.close()
        session = null
        _isConnected.value = false
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (_isConnected.value) {
                delay(15_000)
                runCatching {
                    send(Heartbeat(currentTimeMillis()))
                }.onFailure {
                    _isConnected.value = false
                }
            }
        }
    }
}
