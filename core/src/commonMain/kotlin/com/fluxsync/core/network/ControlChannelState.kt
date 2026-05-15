package com.fluxsync.core.network

sealed class ControlChannelState {
    data object Connected : ControlChannelState()
    data class Reconnecting(val attempt: Int) : ControlChannelState()
    data object Disconnected : ControlChannelState()
    data object Failed : ControlChannelState()
}
