package com.fluxsync.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.fluxsync.core.logging.LogEntry
import com.fluxsync.core.logging.LogLevel
import com.fluxsync.ui.theme.FluxColors
import com.fluxsync.ui.theme.FluxType
import com.fluxsync.ui.theme.Spacing

@Composable
fun ConsoleLog(entries: List<LogEntry>, modifier: Modifier = Modifier) {
    val state = rememberLazyListState()
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) state.animateScrollToItem(entries.lastIndex)
    }
    LazyColumn(modifier = modifier, state = state, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        items(entries) { entry ->
            Text(
                text = "[${entry.timestamp}] [${entry.channel}] [${entry.level.name}] - ${entry.message}",
                color = entry.level.color(),
                style = FluxType.LabelMono,
            )
        }
    }
}

private fun LogLevel.color() = when (this) {
    LogLevel.ERROR -> FluxColors.Error
    LogLevel.WARN -> FluxColors.Warning
    LogLevel.INFO -> FluxColors.Primary
    LogLevel.DEBUG -> FluxColors.OnSurface
}
