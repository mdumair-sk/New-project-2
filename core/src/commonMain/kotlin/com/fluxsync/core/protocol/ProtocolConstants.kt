package com.fluxsync.core.protocol

object ProtocolConstants {
    const val WS_PORT = 8765
    const val DATA_PORT = 8766
    const val MDNS_SERVICE_TYPE = "_fluxsync._tcp.local."
    const val WS_RECONNECT_INTERVAL_MS = 2_000L
    const val WS_RECONNECT_TIMEOUT_MS = 30_000L
    const val PIN_EXPIRY_MS = 60_000L
    const val DEFAULT_CHUNK_SIZE = 2_097_152
    const val MIN_CHUNK_SIZE = 524_288
    const val MAX_CHUNK_SIZE = 16_777_216
    const val LOG_MAX_FILE_SIZE_BYTES = 5_242_880L
    const val LOG_MAX_SESSION_FILES = 3
}
