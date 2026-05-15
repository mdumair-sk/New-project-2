package com.fluxsync.core.platform

expect fun currentTimestampFormatted(): String

expect fun currentTimeMillis(): Long

expect fun appendToFile(path: String, text: String)

expect fun fileSize(path: String): Long

expect fun rotateFile(path: String, rotatedPath: String)

expect fun deleteFile(path: String)

expect fun fileExists(path: String): Boolean
