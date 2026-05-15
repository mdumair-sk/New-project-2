package com.fluxsync.core.network

import com.fluxsync.core.model.ChunkPayload

object ChunkFraming {
    fun encode(payload: ChunkPayload): ByteArray {
        val transferIdBytes = payload.transferId.encodeToByteArray()
        val result = ByteArray(4 + transferIdBytes.size + 4 + 4 + payload.data.size)
        var cursor = 0
        cursor = writeInt(result, cursor, transferIdBytes.size)
        transferIdBytes.copyInto(result, cursor)
        cursor += transferIdBytes.size
        cursor = writeInt(result, cursor, payload.sequenceId)
        cursor = writeInt(result, cursor, payload.data.size)
        payload.data.copyInto(result, cursor)
        return result
    }

    fun decode(bytes: ByteArray): ChunkPayload {
        var cursor = 0
        val idLength = readInt(bytes, cursor)
        cursor += 4
        val transferId = bytes.copyOfRange(cursor, cursor + idLength).decodeToString()
        cursor += idLength
        val sequenceId = readInt(bytes, cursor)
        cursor += 4
        val dataLength = readInt(bytes, cursor)
        cursor += 4
        val data = bytes.copyOfRange(cursor, cursor + dataLength)
        return ChunkPayload(transferId, sequenceId, data)
    }

    private fun writeInt(target: ByteArray, offset: Int, value: Int): Int {
        target[offset] = (value ushr 24).toByte()
        target[offset + 1] = (value ushr 16).toByte()
        target[offset + 2] = (value ushr 8).toByte()
        target[offset + 3] = value.toByte()
        return offset + 4
    }

    private fun readInt(source: ByteArray, offset: Int): Int {
        return ((source[offset].toInt() and 0xFF) shl 24) or
            ((source[offset + 1].toInt() and 0xFF) shl 16) or
            ((source[offset + 2].toInt() and 0xFF) shl 8) or
            (source[offset + 3].toInt() and 0xFF)
    }
}
