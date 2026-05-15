package com.fluxsync.core.engine

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AckTracker {
    private val mutex = Mutex()
    private val acked = mutableMapOf<String, MutableSet<Int>>()
    val retryChannel: Channel<Int> = Channel(Channel.BUFFERED)

    private val _ackedEvents = MutableSharedFlow<Pair<String, Int>>(extraBufferCapacity = 256)
    val ackedEvents: SharedFlow<Pair<String, Int>> = _ackedEvents

    suspend fun markAcked(transferId: String, sequenceId: Int) {
        mutex.withLock {
            acked.getOrPut(transferId) { mutableSetOf() }.add(sequenceId)
        }
        _ackedEvents.emit(transferId to sequenceId)
    }

    suspend fun markNacked(transferId: String, sequenceId: Int) {
        mutex.withLock {
            acked.getOrPut(transferId) { mutableSetOf() }.remove(sequenceId)
        }
        retryChannel.send(sequenceId)
    }

    suspend fun isComplete(transferId: String, totalChunks: Int): Boolean {
        return mutex.withLock { acked[transferId]?.size == totalChunks }
    }

    suspend fun getAckedCount(transferId: String): Int {
        return mutex.withLock { acked[transferId]?.size ?: 0 }
    }

    suspend fun getUnacknowledged(transferId: String, totalChunks: Int): List<Int> {
        return mutex.withLock {
            val received = acked[transferId].orEmpty()
            (0 until totalChunks).filterNot { it in received }
        }
    }

    suspend fun reset(transferId: String) {
        mutex.withLock {
            acked.remove(transferId)
        }
    }
}
