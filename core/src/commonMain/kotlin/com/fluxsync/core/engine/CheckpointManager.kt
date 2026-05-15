package com.fluxsync.core.engine

import com.fluxsync.core.model.PartFileMeta
import com.fluxsync.core.platform.readMetaJson
import com.fluxsync.core.platform.writeMetaJson

class CheckpointManager {
    fun loadMeta(partPath: String): PartFileMeta? {
        return readMetaJson(metaPathFor(partPath))
    }

    fun saveMeta(partPath: String, meta: PartFileMeta) {
        writeMetaJson(metaPathFor(partPath), meta)
    }

    fun buildResumeRequest(meta: PartFileMeta): List<Int> {
        val received = meta.receivedChunks.toSet()
        return (0 until meta.totalChunks).filterNot { it in received }
    }

    private fun metaPathFor(partPath: String): String {
        return if (partPath.endsWith(".part")) {
            partPath.removeSuffix(".part") + ".meta.json"
        } else {
            "$partPath.meta.json"
        }
    }
}
