package com.fluxsync.core

import com.fluxsync.core.model.ChunkPayload
import com.fluxsync.core.network.ChunkFraming
import kotlin.test.Test
import kotlin.test.assertEquals

class ChunkFramingTest {
    @Test
    fun payloadRoundTrip() {
        val payload = ChunkPayload("transfer", 42, byteArrayOf(1, 2, 3))
        assertEquals(payload, ChunkFraming.decode(ChunkFraming.encode(payload)))
    }
}
