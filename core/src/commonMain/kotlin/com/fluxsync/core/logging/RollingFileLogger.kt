package com.fluxsync.core.logging

import com.fluxsync.core.platform.appendToFile
import com.fluxsync.core.platform.deleteFile
import com.fluxsync.core.platform.fileExists
import com.fluxsync.core.platform.fileSize
import com.fluxsync.core.platform.rotateFile
import com.fluxsync.core.protocol.ProtocolConstants
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RollingFileLogger(
    private val logDir: String,
) {
    private val mutex = Mutex()
    private val activeLogPath = "$logDir/session.log"

    suspend fun write(line: String) {
        mutex.withLock {
            if (fileSize(activeLogPath) >= ProtocolConstants.LOG_MAX_FILE_SIZE_BYTES) {
                rotate()
            }
            appendToFile(activeLogPath, line + "\n")
        }
    }

    private fun rotate() {
        for (index in ProtocolConstants.LOG_MAX_SESSION_FILES downTo 1) {
            val current = "$logDir/session_$index.log"
            val next = "$logDir/session_${index + 1}.log"
            if (index == ProtocolConstants.LOG_MAX_SESSION_FILES) {
                deleteFile(current)
            } else if (fileExists(current)) {
                rotateFile(current, next)
            }
        }
        if (fileExists(activeLogPath)) {
            rotateFile(activeLogPath, "$logDir/session_1.log")
        }
    }
}
