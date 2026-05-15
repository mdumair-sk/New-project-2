package com.fluxsync.core.engine

import com.fluxsync.core.model.AbortReason
import com.fluxsync.core.model.ChunkPayload
import com.fluxsync.core.model.TransferFile
import com.fluxsync.core.model.TransferState
import com.fluxsync.core.platform.readFileChunk
import com.fluxsync.core.protocol.ChunkAck
import com.fluxsync.core.protocol.ControlMessage
import com.fluxsync.core.protocol.Nack
import com.fluxsync.core.protocol.SessionEnd
import com.fluxsync.core.protocol.SessionEndReason
import com.fluxsync.core.protocol.TransferRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TransferSession(
    private val file: TransferFile,
    private val dispatcher: ChunkDispatcher,
    private val ackTracker: AckTracker,
    private val controlSender: suspend (ControlMessage) -> Unit,
    private val scope: CoroutineScope,
    private val awaitPreallocation: Boolean = false,
) {
    private val receiverReady = CompletableDeferred<Unit>()
    private var retryJob: Job? = null
    private var paused = false

    private val _state = MutableStateFlow<TransferState>(TransferState.Idle)
    val state: StateFlow<TransferState> = _state

    fun confirmPreallocation() {
        if (!receiverReady.isCompleted) {
            receiverReady.complete(Unit)
        }
    }

    suspend fun handleControlMessage(message: ControlMessage) {
        when (message) {
            is ChunkAck -> ackTracker.markAcked(message.transferId, message.sequenceId)
            is Nack -> ackTracker.markNacked(message.transferId, message.sequenceId)
            else -> Unit
        }
    }

    suspend fun start() {
        runCatching {
            _state.value = TransferState.Negotiating
            controlSender(
                TransferRequest(
                    transferId = file.transferId,
                    fileName = file.fileName,
                    totalSize = file.totalSize,
                    chunkSize = file.chunkSize,
                    totalChunks = file.totalChunks,
                    xxhash = file.xxhash,
                    relativePath = file.relativePath,
                    destinationPath = file.destinationPath,
                ),
            )

            if (awaitPreallocation) {
                receiverReady.await()
            } else {
                confirmPreallocation()
            }

            _state.value = TransferState.Transferring(emptySet(), file.totalChunks)
            retryJob = scope.launch { retryLoop() }

            for (sequenceId in 0 until file.totalChunks) {
                waitIfPaused()
                dispatchSequence(sequenceId)
            }

            while (!ackTracker.isComplete(file.transferId, file.totalChunks) && scope.isActive) {
                delay(100)
            }
            retryJob?.cancel()
            controlSender(SessionEnd(file.transferId, SessionEndReason.COMPLETE))
            _state.value = TransferState.Complete
        }.onFailure {
            retryJob?.cancel()
            _state.value = TransferState.Aborted(AbortReason.ENGINE_ERROR)
            controlSender(SessionEnd(file.transferId, SessionEndReason.ERROR, it.message))
        }
    }

    suspend fun pause() {
        paused = true
        _state.value = TransferState.PausedReconnect(0)
    }

    suspend fun resume() {
        paused = false
        val acked = ackTracker.getUnacknowledged(file.transferId, file.totalChunks).toSet()
        _state.value = TransferState.Transferring(acked, file.totalChunks)
    }

    suspend fun cancel() {
        retryJob?.cancel()
        controlSender(SessionEnd(file.transferId, SessionEndReason.CANCELLED))
        _state.value = TransferState.Aborted(AbortReason.USER_CANCELLED)
    }

    private suspend fun retryLoop() {
        for (sequenceId in ackTracker.retryChannel) {
            if (!scope.isActive) break
            waitIfPaused()
            dispatchSequence(sequenceId)
        }
    }

    private suspend fun dispatchSequence(sequenceId: Int) {
        val offset = sequenceId.toLong() * file.chunkSize
        val remaining = file.totalSize - offset
        val length = minOf(file.chunkSize.toLong(), remaining).toInt()
        val bytes = readFileChunk(file.sourcePath, offset, length)
        dispatcher.dispatch(ChunkPayload(file.transferId, sequenceId, bytes))
    }

    private suspend fun waitIfPaused() {
        while (paused && scope.isActive) {
            delay(100)
        }
    }
}
