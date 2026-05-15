package com.fluxsync.core

import com.fluxsync.core.engine.AckTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class AckTrackerTest {
    @Test
    fun markAckedRecordsCorrectly() = runTest {
        val tracker = AckTracker()
        tracker.markAcked("a", 1)
        assertEquals(listOf(0, 2), tracker.getUnacknowledged("a", 3))
    }

    @Test
    fun isCompleteOnlyWhenAllChunksAcked() = runTest {
        val tracker = AckTracker()
        tracker.markAcked("a", 0)
        tracker.markAcked("a", 1)
        assertFalse(tracker.isComplete("a", 3))
        tracker.markAcked("a", 2)
        assertTrue(tracker.isComplete("a", 3))
    }

    @Test
    fun getUnacknowledgedReturnsDiff() = runTest {
        val tracker = AckTracker()
        tracker.markAcked("a", 0)
        tracker.markAcked("a", 2)
        assertEquals(listOf(1, 3), tracker.getUnacknowledged("a", 4))
    }

    @Test
    fun resetClearsState() = runTest {
        val tracker = AckTracker()
        tracker.markAcked("a", 0)
        tracker.reset("a")
        assertEquals(listOf(0), tracker.getUnacknowledged("a", 1))
    }
}
