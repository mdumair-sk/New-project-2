package com.fluxsync.core.engine

import com.fluxsync.core.model.ChunkPayload
import com.fluxsync.core.model.TransportLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChunkDispatcher(
    private val scope: CoroutineScope,
    private val retryChannel: Channel<Int>,
    private val sendPayload: suspend (TransportLink, ChunkPayload) -> Unit = { _, _ -> },
) {
    private data class LinkState(
        val link: TransportLink,
        val channel: Channel<ChunkPayload>,
        val inFlight: MutableSet<Int> = mutableSetOf(),
        var sentCount: Long = 0L,
    )

    private val mutex = Mutex()
    private val links = mutableMapOf<String, LinkState>()
    private val _activeLinks = MutableStateFlow<List<TransportLink>>(emptyList())
    val activeLinks: StateFlow<List<TransportLink>> = _activeLinks

    fun registerLink(link: TransportLink) {
        scope.launch {
            val state = LinkState(link, Channel(Channel.BUFFERED))
            mutex.withLock {
                links[link.id]?.channel?.close()
                links[link.id] = state
                _activeLinks.value = links.values.map { it.link }
            }
            consume(state)
        }
    }

    fun removeLink(id: String) {
        scope.launch {
            removeLinkInternal(id, requeue = true)
        }
    }

    suspend fun dispatch(payload: ChunkPayload) {
        val target = mutex.withLock {
            links.values
                .filter { it.link.isActive }
                .minWithOrNull(compareBy<LinkState> { it.channelSize() }.thenBy { it.sentCount })
        } ?: error("No active transport links")

        target.channel.send(payload)
    }

    suspend fun debugQueueDepths(): Map<String, Int> {
        return mutex.withLock { links.mapValues { it.value.channelSize() } }
    }

    private fun LinkState.channelSize(): Int = inFlight.size

    private fun consume(state: LinkState) {
        scope.launch {
            for (payload in state.channel) {
                try {
                    mutex.withLock {
                        state.inFlight.add(payload.sequenceId)
                        state.sentCount += 1
                    }
                    sendPayload(state.link, payload)
                    mutex.withLock {
                        state.inFlight.remove(payload.sequenceId)
                    }
                } catch (throwable: Throwable) {
                    removeLinkInternal(state.link.id, requeue = true)
                    break
                }
            }
        }
    }

    private suspend fun removeLinkInternal(id: String, requeue: Boolean) {
        val state = mutex.withLock {
            val removed = links.remove(id)
            _activeLinks.value = links.values.map { it.link }
            removed
        } ?: return

        state.channel.close()
        if (requeue) {
            state.inFlight.forEach { retryChannel.send(it) }
        }
    }
}
