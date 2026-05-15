package com.fluxsync.core.platform

import java.io.File
import java.io.FileOutputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

actual fun currentTimestampFormatted(): String {
    return ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun appendToFile(path: String, text: String) {
    val file = File(path)
    file.parentFile?.mkdirs()
    FileOutputStream(file, true).bufferedWriter().use { it.write(text) }
}

actual fun fileSize(path: String): Long = File(path).takeIf { it.exists() }?.length() ?: 0L

actual fun rotateFile(path: String, rotatedPath: String) {
    val source = File(path)
    if (!source.exists()) return
    val target = File(rotatedPath)
    target.parentFile?.mkdirs()
    target.delete()
    if (!source.renameTo(target)) {
        throw IllegalStateException("Unable to rotate $path to $rotatedPath")
    }
}

actual fun deleteFile(path: String) {
    File(path).delete()
}

actual fun fileExists(path: String): Boolean = File(path).exists()
