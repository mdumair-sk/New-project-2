package com.fluxsync.core.platform

import com.fluxsync.core.model.PartFileMeta

expect fun readFileChunk(path: String, offset: Long, length: Int): ByteArray

expect fun preallocateFile(path: String, size: Long)

expect fun writeFileChunk(path: String, offset: Long, data: ByteArray)

expect fun writeMetaJson(path: String, meta: PartFileMeta)

expect fun readMetaJson(path: String): PartFileMeta?

expect fun computeXxHash(path: String): String

expect fun renameFile(from: String, to: String)

expect fun ensureDirectory(path: String)
