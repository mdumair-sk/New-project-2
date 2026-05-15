package com.fluxsync.core.platform

import com.fluxsync.core.model.PartFileMeta
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
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
    val target = Path.of(path)
    if (!Files.exists(target)) Files.createFile(target)
    runCatching {
        Files.newByteChannel(target, StandardOpenOption.WRITE, StandardOpenOption.SPARSE).use { channel ->
            if (size > 0) {
                channel.position(size - 1)
                channel.write(ByteBuffer.wrap(byteArrayOf(0)))
            }
        }
    }.getOrElse {
        RandomAccessFile(path, "rw").use { file -> file.setLength(size) }
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
    val target = Path.of(path)
    val temp = Path.of("$path.tmp")
    Files.writeString(temp, Json.encodeToString(meta))
    runCatching {
        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }.getOrElse {
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
    }
}

actual fun readMetaJson(path: String): PartFileMeta? {
    val target = Path.of(path)
    if (!Files.exists(target)) return null
    return Json.decodeFromString<PartFileMeta>(Files.readString(target))
}

actual fun computeXxHash(path: String): String {
    val hash = net.openhft.hashing.LongHashFunction.xx().hashBytes(File(path).readBytes())
    return java.lang.Long.toUnsignedString(hash, 16).padStart(16, '0')
}

actual fun renameFile(from: String, to: String) {
    ensureParent(to)
    Files.move(Path.of(from), Path.of(to), StandardCopyOption.REPLACE_EXISTING)
}

actual fun ensureDirectory(path: String) {
    if (path.isBlank()) return
    Files.createDirectories(Path.of(path))
}

private fun ensureParent(path: String) {
    Path.of(path).parent?.let { Files.createDirectories(it) }
}
