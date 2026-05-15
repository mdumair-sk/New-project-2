package com.fluxsync.ui.viewmodel

import com.fluxsync.core.logging.LogEntry
import com.fluxsync.core.logging.LogLevel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class ConsoleViewModel(
    scope: kotlinx.coroutines.CoroutineScope,
    source: SharedFlow<LogEntry>,
) : FluxViewModel(scope) {
    private val allEntries = mutableListOf<LogEntry>()
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries
    private val _filter = MutableStateFlow<LogLevel?>(null)
    val filter: StateFlow<LogLevel?> = _filter
    val copied = MutableSharedFlow<String>(extraBufferCapacity = 1)

    init {
        launchLogged("CONSOLE") {
            source.collect { entry ->
                allEntries.add(entry)
                publish()
            }
        }
    }

    fun setFilter(level: LogLevel?) = launchLogged("CONSOLE") {
        _filter.value = level
        publish()
    }

    fun clear() = launchLogged("CONSOLE") {
        allEntries.clear()
        publish()
    }

    fun copyText() = launchLogged("CONSOLE") {
        copied.emit(_entries.value.joinToString("\n") { "[${it.timestamp}] [${it.channel}] [${it.level}] - ${it.message}" })
    }

    private fun publish() {
        val level = _filter.value
        _entries.value = allEntries.filter { level == null || it.level == level }.takeLast(1_000)
    }
}
