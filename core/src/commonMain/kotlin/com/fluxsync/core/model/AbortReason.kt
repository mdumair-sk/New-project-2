package com.fluxsync.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class AbortReason {
    WS_TIMEOUT,
    CHECKSUM_MISMATCH,
    USER_CANCELLED,
    ENGINE_ERROR,
}
