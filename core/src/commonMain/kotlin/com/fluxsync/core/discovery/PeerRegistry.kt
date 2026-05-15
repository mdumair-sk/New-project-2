package com.fluxsync.core.discovery

import com.fluxsync.core.model.Peer
import com.fluxsync.core.settings.SettingsKeys
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PeerRegistry(
    private val settings: Settings,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) {
    private val peers = mutableMapOf<String, Peer>()
    private val _all = MutableStateFlow<List<Peer>>(emptyList())

    init {
        settings.getStringOrNull(SettingsKeys.TRUSTED_PEERS_JSON)?.let { raw ->
            runCatching { json.decodeFromString<List<Peer>>(raw) }
                .getOrDefault(emptyList())
                .forEach { peers[it.id] = it }
            publish()
        }
    }

    fun addOrUpdate(peer: Peer) {
        peers[peer.id] = peer
        persistTrusted()
        publish()
    }

    fun remove(peerId: String) {
        peers.remove(peerId)
        persistTrusted()
        publish()
    }

    fun getTrusted(): List<Peer> = peers.values.filter { it.isTrusted }

    fun getAll(): StateFlow<List<Peer>> = _all

    private fun publish() {
        _all.value = peers.values.sortedBy { it.moniker }
    }

    private fun persistTrusted() {
        val trusted = peers.values.filter { it.isTrusted }
        settings.putString(SettingsKeys.TRUSTED_PEERS_JSON, json.encodeToString(trusted))
    }
}
