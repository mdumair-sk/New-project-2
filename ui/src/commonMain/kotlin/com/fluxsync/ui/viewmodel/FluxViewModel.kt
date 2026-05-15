package com.fluxsync.ui.viewmodel

import com.fluxsync.core.logging.LogLevel
import com.fluxsync.core.logging.LogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class FluxViewModel(
    protected val scope: CoroutineScope,
) {
    protected fun launchLogged(channel: String, block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }
                .onFailure { LogManager.log(channel, LogLevel.ERROR, "ViewModel operation failed", it) }
        }
    }
}
