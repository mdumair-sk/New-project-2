package com.fluxsync.core.platform

import com.fluxsync.core.model.PartFileMeta
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

actual fun readFileChunk(path: String, offset: Long, length: Int): ByteArray {
    RandomAccessFile(path, "r").use { file ->
        file.seek(offset)
        val buffer = ByteArray(length)
        file.readFully(buffer)
        return buffer
    }
}

actual fun preallocateFile(path: String, size: Long) {
    ensureParent(path)
    RandomAccessFile(path, "rw").use { file ->
        file.setLength(size)
    }
}

actual fun writeFileChunk(path: String, offset: Long, data: ByteArray) {
    ensureParent(path)
    RandomAccessFile(path, "rw").use { file ->
        file.seek(offset)
        file.write(data)
    }
}

actual fun writeMetaJson(path: String, meta: PartFileMeta) {
    ensureParent(path)
    val target = File(path)
    val temp = File("$path.tmp")
    temp.writeText(Json.encodeToString(meta))
    if (!temp.renameTo(target)) {
        target.delete()
        if (!temp.renameTo(target)) {
            throw IllegalStateException("Unable to atomically write metadata: $path")
        }
    }
}

actual fun readMetaJson(path: String): PartFileMeta? {
    val file = File(path)
    if (!file.exists()) return null
    return Json.decodeFromString<PartFileMeta>(file.readText())
}

actual fun computeXxHash(path: String): String {
    val hash = net.openhft.hashing.LongHashFunction.xx().hashBytes(File(path).readBytes())
    return java.lang.Long.toUnsignedString(hash, 16).padStart(16, '0')
}

actual fun renameFile(from: String, to: String) {
    ensureParent(to)
    val source = File(from)
    val target = File(to)
    if (!source.renameTo(target)) {
        throw IllegalStateException("Unable to rename $from to $to")
    }
}

actual fun ensureDirectory(path: String) {
    if (path.isBlank()) return
    File(path).mkdirs()
}

private fun ensureParent(path: String) {
    File(path).parentFile?.mkdirs()
}
