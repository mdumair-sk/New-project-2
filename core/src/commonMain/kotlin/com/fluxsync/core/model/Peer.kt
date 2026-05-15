package com.fluxsync.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Peer(
    val id: String,
    val moniker: String,
    val isTrusted: Boolean,
    val address: String,
    val port: Int,
)
