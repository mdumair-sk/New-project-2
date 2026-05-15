package com.fluxsync.core

import com.fluxsync.core.protocol.ChunkAck
import com.fluxsync.core.protocol.ControlJson
import com.fluxsync.core.protocol.ControlMessage
import com.fluxsync.core.protocol.Heartbeat
import com.fluxsync.core.protocol.Nack
import com.fluxsync.core.protocol.NackReason
import com.fluxsync.core.protocol.SessionEnd
import com.fluxsync.core.protocol.SessionEndReason
import com.fluxsync.core.protocol.TransferRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString

class ControlMessageSerializerTest {
    @Test
    fun subtypesRoundTrip() {
        val messages = listOf<ControlMessage>(
            TransferRequest("id", "a.bin", 10, 2, 5, "hash", "a.bin"),
            ChunkAck("id", 1),
            Nack("id", 2, NackReason.CHECKSUM_MISMATCH),
            Heartbeat(1L),
            SessionEnd("id", SessionEndReason.COMPLETE),
        )
        messages.forEach { message ->
            val raw = ControlJson.instance.encodeToString(ControlMessage.serializer(), message)
            assertEquals(message, ControlJson.instance.decodeFromString(ControlMessage.serializer(), raw))
        }
    }

    @Test
    fun unknownTypeThrows() {
        assertFailsWith<SerializationException> {
            ControlJson.instance.decodeFromString(ControlMessage.serializer(), """{"type":"NOPE"}""")
        }
    }
}
