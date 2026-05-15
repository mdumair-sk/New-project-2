package com.fluxsync.core.logging

import com.fluxsync.core.platform.currentTimestampFormatted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

object LogManager {
    private var logger: RollingFileLogger? = null
    private var scope: CoroutineScope? = null
    private val _entries = MutableSharedFlow<LogEntry>(replay = 128, extraBufferCapacity = 256)
    val entries: SharedFlow<LogEntry> = _entries

    fun init(logDir: String, scope: CoroutineScope) {
        this.scope = scope
        logger = RollingFileLogger(logDir)
    }

    fun log(channel: String, level: LogLevel, message: String, throwable: Throwable? = null) {
        val timestamp = currentTimestampFormatted()
        val detail = throwable?.let { ": ${it.message}" }.orEmpty()
        val text = "$message$detail"
        val formatted = "[$timestamp] [$channel] [${level.name}] - $text"
        val entry = LogEntry(timestamp, channel, level, text)
        _entries.tryEmit(entry)
        scope?.launch {
            logger?.write(formatted)
        }
    }
}
