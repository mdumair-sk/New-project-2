package com.fluxsync.core

import com.fluxsync.core.model.PartFileMeta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PartFileMetaSerializationTest {
    @Test
    fun roundTrip() {
        val meta = PartFileMeta("id", "video.mp4", 3, 1024, "abc", listOf(0, 2))
        val raw = Json.encodeToString(meta)
        assertEquals(meta, Json.decodeFromString<PartFileMeta>(raw))
    }
}
