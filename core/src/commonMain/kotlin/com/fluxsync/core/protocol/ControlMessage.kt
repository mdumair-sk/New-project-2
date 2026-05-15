package com.fluxsync.core.protocol

import kotlinx.serialization.Serializable

@Serializable(with = ControlMessageSerializer::class)
sealed class ControlMessage {
    abstract val type: String
}
