package com.fluxsync.core.engine

import com.fluxsync.core.model.AbortReason
import com.fluxsync.core.model.ChunkPayload
import com.fluxsync.core.model.PartFileMeta
import com.fluxsync.core.model.TransferState
import com.fluxsync.core.platform.computeXxHash
import com.fluxsync.core.platform.ensureDirectory
import com.fluxsync.core.platform.preallocateFile
import com.fluxsync.core.platform.renameFile
import com.fluxsync.core.platform.writeFileChunk
import com.fluxsync.core.platform.writeMetaJson
import com.fluxsync.core.protocol.ChunkAck
import com.fluxsync.core.protocol.ControlMessage
import com.fluxsync.core.protocol.Nack
import com.fluxsync.core.protocol.NackReason
import com.fluxsync.core.protocol.SessionEnd
import com.fluxsync.core.protocol.SessionEndReason
import com.fluxsync.core.protocol.TransferRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ReceiverSession(
    private val request: TransferRequest,
    private val controlSender: suspend (ControlMessage) -> Unit,
) {
    private val received = mutableSetOf<Int>()
    private val partPath = "${request.destinationPath}.part"
    private val metaPath = "${request.destinationPath}.meta.json"

    private val _state = MutableStateFlow<TransferState>(TransferState.Idle)
    val state: StateFlow<TransferState> = _state

    suspend fun start() {
        runCatching {
            _state.value = TransferState.Negotiating
            ensureDirectory(request.destinationPath.substringBeforeLast("/", missingDelimiterValue = ""))
            preallocateFile(partPath, request.totalSize)
            persistMeta()
            _state.value = TransferState.Transferring(emptySet(), request.totalChunks)
        }.onFailure {
            controlSender(SessionEnd(request.transferId, SessionEndReason.ERROR, it.message))
            _state.value = TransferState.Aborted(AbortReason.ENGINE_ERROR)
        }
    }

    suspend fun writeChunk(payload: ChunkPayload) {
        if (payload.transferId != request.transferId) return
        runCatching {
            val offset = payload.sequenceId.toLong() * request.chunkSize
            writeFileChunk(partPath, offset, payload.data)
            received.add(payload.sequenceId)
            persistMeta()
            controlSender(ChunkAck(request.transferId, payload.sequenceId))
            _state.value = TransferState.Transferring(received.toSet(), request.totalChunks)
            if (received.size == request.totalChunks) {
                verify()
            }
        }.onFailure {
            controlSender(Nack(request.transferId, payload.sequenceId, NackReason.DISK_WRITE_FAILURE))
        }
    }

    suspend fun verify() {
        runCatching {
            _state.value = TransferState.Verifying
            val actual = computeXxHash(partPath)
            if (actual.equals(request.xxhash, ignoreCase = true)) {
                _state.value = TransferState.Finalizing
                renameFile(partPath, request.destinationPath)
                controlSender(SessionEnd(request.transferId, SessionEndReason.COMPLETE))
                _state.value = TransferState.Complete
            } else {
                controlSender(SessionEnd(request.transferId, SessionEndReason.ERROR, "CHECKSUM_MISMATCH"))
                _state.value = TransferState.Aborted(AbortReason.CHECKSUM_MISMATCH)
            }
        }.onFailure {
            controlSender(SessionEnd(request.transferId, SessionEndReason.ERROR, it.message))
            _state.value = TransferState.Aborted(AbortReason.ENGINE_ERROR)
        }
    }

    private fun currentMeta(): PartFileMeta {
        return PartFileMeta(
            transferId = request.transferId,
            fileName = request.fileName,
            totalChunks = request.totalChunks,
            chunkSize = request.chunkSize,
            xxhash = request.xxhash,
            receivedChunks = received.sorted(),
        )
    }

    private fun persistMeta() {
        writeMetaJson(metaPath, currentMeta())
    }
}
