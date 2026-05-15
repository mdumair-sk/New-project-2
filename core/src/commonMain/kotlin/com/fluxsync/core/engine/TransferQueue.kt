package com.fluxsync.core.engine

import com.fluxsync.core.model.TransferFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TransferQueue {
    private val mutex = Mutex()
    private val queue = ArrayDeque<TransferFile>()
    private val _size = MutableStateFlow(0)
    val size: StateFlow<Int> = _size

    suspend fun enqueue(file: TransferFile) {
        mutex.withLock {
            queue.addLast(file)
            _size.value = queue.size
        }
    }

    suspend fun peek(): TransferFile? = mutex.withLock { queue.firstOrNull() }

    suspend fun dequeue(): TransferFile? {
        return mutex.withLock {
            val value = queue.removeFirstOrNull()
            _size.value = queue.size
            value
        }
    }

    suspend fun snapshot(): List<TransferFile> = mutex.withLock { queue.toList() }

    suspend fun cancel(transferId: String): Boolean {
        return mutex.withLock {
            val removed = queue.removeAll { it.transferId == transferId }
            _size.value = queue.size
            removed
        }
    }
}
