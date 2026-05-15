package com.fluxsync.ui.viewmodel

import com.fluxsync.core.pairing.TrustStore
import com.fluxsync.core.pairing.TrustedRecord
import com.fluxsync.core.protocol.ProtocolConstants
import com.fluxsync.core.settings.SettingsKeys
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    scope: kotlinx.coroutines.CoroutineScope,
    private val settings: Settings,
    private val trustStore: TrustStore,
) : FluxViewModel(scope) {
    private val _chunkSizeBytes = MutableStateFlow(settings.getInt(SettingsKeys.CHUNK_SIZE_BYTES, ProtocolConstants.DEFAULT_CHUNK_SIZE))
    val chunkSizeBytes: StateFlow<Int> = _chunkSizeBytes
    private val _speedRefreshFast = MutableStateFlow(settings.getBoolean(SettingsKeys.SPEED_REFRESH_FAST, true))
    val speedRefreshFast: StateFlow<Boolean> = _speedRefreshFast
    private val _trustedDevices = MutableStateFlow(trustStore.getAll())
    val trustedDevices: StateFlow<List<TrustedRecord>> = _trustedDevices

    fun setChunkSize(bytes: Int) = launchLogged("SETTINGS") {
        val snapped = snapChunkSize(bytes)
        settings.putInt(SettingsKeys.CHUNK_SIZE_BYTES, snapped)
        _chunkSizeBytes.value = snapped
    }

    fun toggleSpeedRefresh() = launchLogged("SETTINGS") {
        val next = !_speedRefreshFast.value
        settings.putBoolean(SettingsKeys.SPEED_REFRESH_FAST, next)
        _speedRefreshFast.value = next
    }

    fun removeTrustedDevice(peerId: String) = launchLogged("SETTINGS") {
        trustStore.delete(peerId)
        _trustedDevices.value = trustStore.getAll()
    }

    private fun snapChunkSize(bytes: Int): Int {
        return listOf(524_288, 1_048_576, 2_097_152, 4_194_304, 8_388_608, 16_777_216)
            .minBy { kotlin.math.abs(it - bytes) }
    }
}
