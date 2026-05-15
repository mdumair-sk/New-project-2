package com.fluxsync.ui.viewmodel

import com.fluxsync.core.engine.AckTracker
import com.fluxsync.core.engine.ChunkDispatcher
import com.fluxsync.core.engine.PartFileScanner
import com.fluxsync.core.engine.TransferQueue
import com.fluxsync.core.logging.LogEntry
import com.fluxsync.core.logging.LogManager
import com.fluxsync.core.model.TransferFile
import com.fluxsync.core.model.TransferState
import com.fluxsync.core.platform.currentTimeMillis
import com.fluxsync.core.protocol.ProtocolConstants
import com.fluxsync.ui.screens.AppNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TransferViewModel(
    scope: kotlinx.coroutines.CoroutineScope,
    private val ackTracker: AckTracker,
    private val dispatcher: ChunkDispatcher,
    private val transferQueue: TransferQueue,
    private val scanner: PartFileScanner,
    private val navigator: AppNavigator,
    private val transfersDirectory: String,
    private val speedRefreshFast: StateFlow<Boolean> = MutableStateFlow(true),
    private val selectFiles: suspend () -> List<String> = { emptyList() },
    private val selectFolder: suspend () -> String? = { null },
) : FluxViewModel(scope) {
    private val _transferState = MutableStateFlow<TransferState>(TransferState.Idle)
    val transferState: StateFlow<TransferState> = _transferState
    private val _speedBytesPerSec = MutableStateFlow(0L)
    val speedBytesPerSec: StateFlow<Long> = _speedBytesPerSec
    private val _etaSeconds = MutableStateFlow(0L)
    val etaSeconds: StateFlow<Long> = _etaSeconds
    private val _queue = MutableStateFlow<List<TransferFile>>(emptyList())
    val queue: StateFlow<List<TransferFile>> = _queue
    private val _partFilesBytes = MutableStateFlow(0L)
    val partFilesBytes: StateFlow<Long> = _partFilesBytes
    private val _adbAvailable = MutableStateFlow(true)
    val adbAvailable: StateFlow<Boolean> = _adbAvailable
    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries
    private val _ackedBytes = MutableStateFlow(0L)

    init {
        launchLogged("TRANSFER") {
            LogManager.entries.collect { entry ->
                _logEntries.value = (_logEntries.value + entry).takeLast(500)
            }
        }
        launchLogged("TRANSFER") {
            ackTracker.ackedEvents.collect {
                val chunkSize = _queue.value.firstOrNull()?.chunkSize ?: ProtocolConstants.DEFAULT_CHUNK_SIZE
                _ackedBytes.value += chunkSize
            }
        }
        launchLogged("TRANSFER") { speedLoop() }
        refreshPartFiles()
    }

    fun addFiles(paths: List<String>) = launchLogged("TRANSFER") {
        paths.forEach { path -> transferQueue.enqueue(path.toTransferFile()) }
        refreshQueue()
    }

    fun addFolder(path: String) = launchLogged("TRANSFER") {
        transferQueue.enqueue(path.toTransferFile(isFolder = true))
        refreshQueue()
    }

    fun requestAddFiles() = launchLogged("TRANSFER") {
        addFiles(selectFiles())
    }

    fun requestAddFolder() = launchLogged("TRANSFER") {
        selectFolder()?.let { addFolder(it) }
    }

    fun cancelCurrent() = launchLogged("TRANSFER") {
        val current = _queue.value.firstOrNull() ?: return@launchLogged
        transferQueue.cancel(current.transferId)
        _transferState.value = TransferState.Aborted(com.fluxsync.core.model.AbortReason.USER_CANCELLED)
        refreshQueue()
    }

    fun emptyTrash() = launchLogged("TRANSFER") {
        val infos = scanner.scanDirectory(transfersDirectory)
        scanner.deleteAll(infos)
        refreshPartFiles()
    }

    fun setAdbAvailable(value: Boolean) {
        _adbAvailable.value = value
    }

    private suspend fun speedLoop() {
        var previousBytes = 0L
        var previousTime = currentTimeMillis()
        while (true) {
            val intervalMs = if (speedRefreshFast.value) 500L else 2_000L
            delay(intervalMs)
            val now = currentTimeMillis()
            val bytes = _ackedBytes.value
            val deltaBytes = bytes - previousBytes
            val deltaMillis = (now - previousTime).coerceAtLeast(1L)
            val speed = (deltaBytes * 1000L) / deltaMillis
            _speedBytesPerSec.value = speed
            val total = _queue.value.firstOrNull()?.totalSize ?: 0L
            _etaSeconds.value = if (speed > 0 && total > bytes) (total - bytes) / speed else 0L
            previousBytes = bytes
            previousTime = now
        }
    }

    private suspend fun refreshQueue() {
        _queue.value = transferQueue.snapshot()
    }

    private fun refreshPartFiles() {
        launchLogged("TRANSFER") {
            val infos = scanner.scanDirectory(transfersDirectory)
            _partFilesBytes.value = scanner.totalBytes(infos)
        }
    }

    private fun String.toTransferFile(isFolder: Boolean = false): TransferFile {
        val normalized = replace('\\', '/')
        val name = normalized.substringAfterLast('/').ifBlank { normalized }
        val id = "local-${currentTimeMillis()}-${normalized.hashCode()}"
        return TransferFile(
            transferId = id,
            fileName = if (isFolder) "$name/" else name,
            relativePath = name,
            sourcePath = this,
            destinationPath = "$transfersDirectory/$name",
            totalSize = 0L,
            chunkSize = ProtocolConstants.DEFAULT_CHUNK_SIZE,
            totalChunks = 0,
            xxhash = "",
            state = TransferState.Idle,
        )
    }
}
