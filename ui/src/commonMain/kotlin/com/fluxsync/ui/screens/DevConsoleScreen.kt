package com.fluxsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.fluxsync.core.logging.LogLevel
import com.fluxsync.ui.Strings
import com.fluxsync.ui.components.ConsoleLog
import com.fluxsync.ui.theme.FluxColors
import com.fluxsync.ui.theme.FluxType
import com.fluxsync.ui.theme.Spacing
import com.fluxsync.ui.viewmodel.ConsoleViewModel

@Composable
fun DevConsoleScreen(viewModel: ConsoleViewModel, modifier: Modifier = Modifier) {
    val entries by viewModel.entries.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        viewModel.copied.collect { clipboard.setText(AnnotatedString(it)) }
    }

    Column(
        modifier = modifier.fillMaxSize().background(FluxColors.Background).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(Strings.DevConsole, color = FluxColors.OnBackground, style = FluxType.DisplaySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilterChip(selected = filter == null, onClick = { viewModel.setFilter(null) }, label = { Text(Strings.AllFilter) })
            FilterChip(selected = filter == LogLevel.ERROR, onClick = { viewModel.setFilter(LogLevel.ERROR) }, label = { Text(Strings.Error) })
            FilterChip(selected = filter == LogLevel.WARN, onClick = { viewModel.setFilter(LogLevel.WARN) }, label = { Text(Strings.Warn) })
            FilterChip(selected = filter == LogLevel.INFO, onClick = { viewModel.setFilter(LogLevel.INFO) }, label = { Text(Strings.Info) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(onClick = viewModel::copyText) { Text(Strings.Clipboard) }
            Button(onClick = viewModel::clear) { Text(Strings.Clear) }
        }
        ConsoleLog(entries = entries, modifier = Modifier.weight(1f))
    }
}
