package com.fluxsync.ui.viewmodel

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.fluxsync.core.logging.LogEntry
import com.fluxsync.core.logging.LogLevel
import com.fluxsync.core.platform.currentTimestampFormatted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class KermitLogSink : LogWriter() {
    private val _entries = MutableSharedFlow<LogEntry>(replay = 128, extraBufferCapacity = 256)
    val entries: SharedFlow<LogEntry> = _entries

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        _entries.tryEmit(
            LogEntry(
                timestamp = currentTimestampFormatted(),
                channel = tag.ifBlank { "APP" },
                level = severity.toFluxLevel(),
                message = throwable?.message?.let { "$message: $it" } ?: message,
            ),
        )
    }

    private fun Severity.toFluxLevel(): LogLevel = when (this) {
        Severity.Verbose -> LogLevel.DEBUG
        Severity.Debug -> LogLevel.DEBUG
        Severity.Info -> LogLevel.INFO
        Severity.Warn -> LogLevel.WARN
        Severity.Error -> LogLevel.ERROR
        Severity.Assert -> LogLevel.ERROR
    }
}
