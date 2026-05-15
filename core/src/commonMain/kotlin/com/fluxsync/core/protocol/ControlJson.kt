package com.fluxsync.core.protocol

import kotlinx.serialization.json.Json

object ControlJson {
    val instance: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        classDiscriminator = "messageClass"
    }
}
