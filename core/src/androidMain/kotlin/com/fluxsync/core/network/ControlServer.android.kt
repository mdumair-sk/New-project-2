package com.fluxsync.core.network

import com.fluxsync.core.protocol.ControlJson
import com.fluxsync.core.protocol.ControlMessage
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.encodeToString

actual class ControlServer actual constructor() {
    private val _incoming = MutableSharedFlow<ControlMessage>(replay = 1, extraBufferCapacity = 64)
    private var engine: ApplicationEngine? = null
    private var session: WebSocketSession? = null

    actual val incoming: SharedFlow<ControlMessage> = _incoming

    actual suspend fun start(port: Int) {
        if (engine != null) return
        engine = embeddedServer(CIO, port = port) {
            install(WebSockets)
            routing {
                webSocket("/control") {
                    if (session != null) {
                        close()
                        return@webSocket
                    }
                    session = this
                    try {
                        for (frame in incoming) {
                            val raw = (frame as? Frame.Text)?.readText() ?: continue
                            _incoming.emit(ControlJson.instance.decodeFromString(ControlMessage.serializer(), raw))
                        }
                    } finally {
                        session = null
                    }
                }
            }
        }.start(wait = false)
    }

    actual suspend fun send(message: ControlMessage) {
        val raw = ControlJson.instance.encodeToString(ControlMessage.serializer(), message)
        session?.send(Frame.Text(raw))
    }

    actual suspend fun stop() {
        session?.close()
        session = null
        engine?.stop(1000, 2000)
        engine = null
    }
}
