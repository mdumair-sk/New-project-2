package com.fluxsync.core.model

import kotlinx.serialization.Serializable

@Serializable
sealed class TransferState {
    @Serializable
    data object Idle : TransferState()

    @Serializable
    data object Negotiating : TransferState()

    @Serializable
    data class Transferring(
        val receivedChunks: Set<Int>,
        val totalChunks: Int,
    ) : TransferState()

    @Serializable
    data class PausedReconnect(val attemptCount: Int) : TransferState()

    @Serializable
    data object Verifying : TransferState()

    @Serializable
    data object Finalizing : TransferState()

    @Serializable
    data object Complete : TransferState()

    @Serializable
    data class Aborted(val reason: AbortReason) : TransferState()
}
