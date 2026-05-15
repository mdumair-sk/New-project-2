package com.fluxsync.core

import com.fluxsync.core.engine.AckTracker
import com.fluxsync.core.engine.ChunkDispatcher
import com.fluxsync.core.model.ChunkPayload
import com.fluxsync.core.model.LinkType
import com.fluxsync.core.model.PlatformSocket
import com.fluxsync.core.model.TransportLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class ChunkDispatcherTest {
    @Test
    fun dispatchesToSingleLink() = runTest {
        val tracker = AckTracker()
        val sent = mutableListOf<String>()
        val dispatcher = ChunkDispatcher(this, tracker.retryChannel) { link, _ -> sent.add(link.id) }
        dispatcher.registerLink(TransportLink("a", LinkType.WIFI, PlatformSocket(), true))
        runCurrent()
        dispatcher.dispatch(ChunkPayload("t", 0, byteArrayOf(1)))
        runCurrent()
        assertEquals(listOf("a"), sent)
    }

    @Test
    fun dispatchesAcrossMultipleLinks() = runTest {
        val tracker = AckTracker()
        val sent = mutableListOf<String>()
        val dispatcher = ChunkDispatcher(this, tracker.retryChannel) { link, _ -> sent.add(link.id) }
        dispatcher.registerLink(TransportLink("a", LinkType.WIFI, PlatformSocket(), true))
        dispatcher.registerLink(TransportLink("b", LinkType.ADB, PlatformSocket(), true))
        runCurrent()
        dispatcher.dispatch(ChunkPayload("t", 0, byteArrayOf(1)))
        runCurrent()
        dispatcher.dispatch(ChunkPayload("t", 1, byteArrayOf(2)))
        runCurrent()
        assertEquals(setOf("a", "b"), sent.toSet())
    }

    @Test
    fun failedLinkRequeuesInFlightChunk() = runTest {
        val tracker = AckTracker()
        val dispatcher = ChunkDispatcher(this, tracker.retryChannel) { _, _ -> error("pipe failed") }
        dispatcher.registerLink(TransportLink("a", LinkType.WIFI, PlatformSocket(), true))
        runCurrent()
        dispatcher.dispatch(ChunkPayload("t", 7, byteArrayOf(1)))
        runCurrent()
        assertEquals(7, tracker.retryChannel.tryReceive().getOrNull())
    }
}
