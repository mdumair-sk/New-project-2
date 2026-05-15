package com.fluxsync.core.engine

data class PartFileInfo(
    val partPath: String,
    val metaPath: String?,
    val sizeBytes: Long,
)

expect class PartFileScanner() {
    fun scanDirectory(path: String): List<PartFileInfo>
    fun totalBytes(infos: List<PartFileInfo>): Long
    fun deleteAll(infos: List<PartFileInfo>)
}
