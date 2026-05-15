package com.fluxsync.core.network

import com.fluxsync.core.model.AbortReason
import com.fluxsync.core.protocol.ControlMessage
import com.fluxsync.core.protocol.ProtocolConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ControlChannelManager(
    private val delegate: ControlChannel,
    private val scope: CoroutineScope,
) : ControlChannel {
    private val _state = MutableStateFlow<ControlChannelState>(ControlChannelState.Disconnected)
    private val _abortReasons = MutableSharedFlow<AbortReason>(replay = 1)
    private var lastAddress: String? = null
    private var lastPort: Int? = null

    val state: StateFlow<ControlChannelState> = _state
    val abortReasons: SharedFlow<AbortReason> = _abortReasons
    override val isConnected: StateFlow<Boolean> = delegate.isConnected

    init {
        scope.launch {
            delegate.isConnected.collect { connected ->
                if (connected) {
                    _state.value = ControlChannelState.Connected
                } else if (_state.value == ControlChannelState.Connected) {
                    reconnect()
                }
            }
        }
    }

    override suspend fun connect(address: String, port: Int) {
        lastAddress = address
        lastPort = port
        delegate.connect(address, port)
    }

    override suspend fun send(message: ControlMessage) = delegate.send(message)

    override fun incoming(): Flow<ControlMessage> = delegate.incoming()

    override suspend fun disconnect() {
        delegate.disconnect()
        _state.value = ControlChannelState.Disconnected
    }

    private suspend fun reconnect() {
        val address = lastAddress ?: return
        val port = lastPort ?: return
        val maxAttempts = (ProtocolConstants.WS_RECONNECT_TIMEOUT_MS / ProtocolConstants.WS_RECONNECT_INTERVAL_MS).toInt()
        repeat(maxAttempts) { index ->
            val attempt = index + 1
            _state.value = ControlChannelState.Reconnecting(attempt)
            delay(ProtocolConstants.WS_RECONNECT_INTERVAL_MS)
            runCatching { delegate.connect(address, port) }
            if (delegate.isConnected.value) {
                _state.value = ControlChannelState.Connected
                return
            }
        }
        _state.value = ControlChannelState.Failed
        _abortReasons.emit(AbortReason.WS_TIMEOUT)
    }
}
