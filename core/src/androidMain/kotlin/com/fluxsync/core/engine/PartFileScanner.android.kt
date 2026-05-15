package com.fluxsync.core.engine

import java.io.File

actual class PartFileScanner actual constructor() {
    actual fun scanDirectory(path: String): List<PartFileInfo> {
        val root = File(path)
        if (!root.exists()) return emptyList()
        return root.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".part") }
            .map { part ->
                val meta = File(part.absolutePath.removeSuffix(".part") + ".meta.json")
                PartFileInfo(
                    partPath = part.absolutePath,
                    metaPath = meta.takeIf { it.exists() }?.absolutePath,
                    sizeBytes = part.length(),
                )
            }
            .toList()
    }

    actual fun totalBytes(infos: List<PartFileInfo>): Long = infos.sumOf { it.sizeBytes }

    actual fun deleteAll(infos: List<PartFileInfo>) {
        infos.forEach {
            File(it.partPath).delete()
            it.metaPath?.let { meta -> File(meta).delete() }
        }
    }
}
