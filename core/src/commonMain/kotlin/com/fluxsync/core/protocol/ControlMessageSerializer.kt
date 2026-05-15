package com.fluxsync.core.protocol

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ControlMessageSerializer : JsonContentPolymorphicSerializer<ControlMessage>(ControlMessage::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ControlMessage> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "TransferRequest" -> TransferRequest.serializer()
            "ChunkACK" -> ChunkAck.serializer()
            "NACK" -> Nack.serializer()
            "Heartbeat" -> Heartbeat.serializer()
            "SessionEnd" -> SessionEnd.serializer()
            else -> throw SerializationException("Unknown control message type: ${element.jsonObject["type"]}")
        }
    }
}
