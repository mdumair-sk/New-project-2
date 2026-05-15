package com.fluxsync.core.desktop

import com.fluxsync.core.protocol.ProtocolConstants
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class AdbEvent {
    data class Connected(val serial: String) : AdbEvent()
    data class Disconnected(val serial: String) : AdbEvent()
    data object AdbNotFound : AdbEvent()
}

class AdbPoller(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val _events = MutableSharedFlow<AdbEvent>(replay = 1, extraBufferCapacity = 32)
    val events: SharedFlow<AdbEvent> = _events
    private val connected = mutableSetOf<String>()

    fun start() {
        scope.launch {
            while (isActive) {
                pollOnce()
                delay(3_000)
            }
        }
    }

    private suspend fun pollOnce() {
        val devices = runCatching { listDevices() }.getOrElse {
            if (it is IOException) _events.emit(AdbEvent.AdbNotFound)
            emptySet()
        }

        devices.filterNot { it in connected }.forEach { serial ->
            connected.add(serial)
            reverse(serial, ProtocolConstants.WS_PORT)
            reverse(serial, ProtocolConstants.DATA_PORT)
            _events.emit(AdbEvent.Connected(serial))
        }

        connected.toList().filterNot { it in devices }.forEach { serial ->
            connected.remove(serial)
            _events.emit(AdbEvent.Disconnected(serial))
        }
    }

    private fun listDevices(): Set<String> {
        val output = ProcessBuilder("adb", "devices").redirectErrorStream(true).start()
            .inputStream.bufferedReader().readText()
        return output.lineSequence()
            .drop(1)
            .map { it.trim() }
            .filter { it.endsWith("device") }
            .map { it.substringBefore('\t').trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun reverse(serial: String, port: Int) {
        runCatching {
            ProcessBuilder("adb", "-s", serial, "reverse", "tcp:$port", "tcp:$port").start().waitFor()
        }
    }
}
