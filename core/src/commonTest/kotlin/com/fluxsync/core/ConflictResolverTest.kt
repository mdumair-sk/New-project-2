package com.fluxsync.core

import com.fluxsync.core.engine.ConflictResolver
import kotlin.test.Test
import kotlin.test.assertEquals

class ConflictResolverTest {
    @Test
    fun noConflictReturnsUnchanged() {
        assertEquals("video.mp4", ConflictResolver.resolve("video.mp4", emptySet()))
    }

    @Test
    fun singleConflictAppendsOne() {
        assertEquals("video(1).mp4", ConflictResolver.resolve("video.mp4", setOf("video.mp4")))
    }

    @Test
    fun multipleConflictsIncrement() {
        val existing = setOf("video.mp4", "video(1).mp4", "video(2).mp4")
        assertEquals("video(3).mp4", ConflictResolver.resolve("video.mp4", existing))
    }

    @Test
    fun noExtensionHandled() {
        assertEquals("README(1)", ConflictResolver.resolve("README", setOf("README")))
    }

    @Test
    fun multipleDotsHandled() {
        assertEquals("archive.tar(1).gz", ConflictResolver.resolve("archive.tar.gz", setOf("archive.tar.gz")))
    }
}
