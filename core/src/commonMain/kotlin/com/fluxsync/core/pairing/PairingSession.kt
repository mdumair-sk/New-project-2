package com.fluxsync.core.pairing

import com.fluxsync.core.platform.currentTimeMillis
import com.fluxsync.core.platform.deriveKey
import com.fluxsync.core.platform.secureRandomBytes
import com.fluxsync.core.protocol.ProtocolConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PairingSession(
    private val scope: CoroutineScope,
) {
    private var currentPin: String? = null
    private var expiryJob: Job? = null
    private var salt: ByteArray = ByteArray(0)

    private val _state = MutableStateFlow<PairingState>(PairingState.Idle)
    val state: StateFlow<PairingState> = _state

    fun generatePin(): String {
        val value = secureRandomBytes(4).fold(0) { acc, byte -> (acc * 31) + (byte.toInt() and 0xFF) }
        return ((value and Int.MAX_VALUE) % 1_000_000).toString().padStart(6, '0')
    }

    fun start(): String {
        val pin = generatePin()
        currentPin = pin
        salt = secureRandomBytes(16)
        val expiresAt = currentTimeMillis() + ProtocolConstants.PIN_EXPIRY_MS
        _state.value = PairingState.AwaitingConfirmation(pin, expiresAt)
        expiryJob?.cancel()
        expiryJob = scope.launch {
            delay(ProtocolConstants.PIN_EXPIRY_MS)
            if (_state.value is PairingState.AwaitingConfirmation) {
                _state.value = PairingState.Expired
                currentPin = null
            }
        }
        return pin
    }

    suspend fun confirmPin(input: String): Boolean {
        val pin = currentPin
        if (pin == null || input != pin) {
            _state.value = PairingState.Failed
            return false
        }

        return runCatching {
            val key = deriveKey(pin, salt)
            currentPin = null
            expiryJob?.cancel()
            _state.value = PairingState.Confirmed(key.copyOf(), salt.copyOf())
            key.fill(0)
            true
        }.getOrElse {
            _state.value = PairingState.Failed
            false
        }
    }
}
