package com.fluxsync.core.model

data class ChunkPayload(
    val transferId: String,
    val sequenceId: Int,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChunkPayload) return false
        return transferId == other.transferId &&
            sequenceId == other.sequenceId &&
            data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = transferId.hashCode()
        result = 31 * result + sequenceId
        result = 31 * result + data.contentHashCode()
        return result
    }
}
