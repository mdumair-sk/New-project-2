package com.fluxsync.core.logging

data class LogEntry(
    val timestamp: String,
    val channel: String,
    val level: LogLevel,
    val message: String,
)
