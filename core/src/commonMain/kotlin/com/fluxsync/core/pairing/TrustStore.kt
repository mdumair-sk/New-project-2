package com.fluxsync.core.pairing

import com.fluxsync.core.settings.SettingsKeys
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TrustedRecord(
    val peerId: String,
    val moniker: String,
    val keyHex: String,
    val saltHex: String,
)

class TrustStore(
    private val settings: Settings,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) {
    private val records = mutableMapOf<String, TrustedRecord>()

    init {
        settings.getStringOrNull(SettingsKeys.TRUSTED_RECORDS_JSON)?.let { raw ->
            runCatching { json.decodeFromString<List<TrustedRecord>>(raw) }
                .getOrDefault(emptyList())
                .forEach { records[it.peerId] = it }
        }
    }

    fun save(record: TrustedRecord) {
        records[record.peerId] = record
        persist()
    }

    fun delete(peerId: String) {
        records.remove(peerId)
        persist()
    }

    fun getAll(): List<TrustedRecord> = records.values.sortedBy { it.moniker }

    fun isTrusted(peerId: String): Boolean = peerId in records

    private fun persist() {
        settings.putString(SettingsKeys.TRUSTED_RECORDS_JSON, json.encodeToString(records.values.toList()))
    }
}
