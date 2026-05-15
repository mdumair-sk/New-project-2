package com.fluxsync.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fluxsync.ui.Strings
import com.fluxsync.ui.theme.FluxColors
import com.fluxsync.ui.theme.Spacing

@Composable
fun StorageBanner(partFilesBytes: Long, onEmptyTrash: () -> Unit, modifier: Modifier = Modifier) {
    if (partFilesBytes == 0L) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "${formatBytes(partFilesBytes)} ${Strings.IncompleteTransfers}", color = FluxColors.Warning)
        Button(onClick = onEmptyTrash) {
            Icon(Icons.Default.Delete, contentDescription = Strings.EmptyTrash)
            Text(Strings.EmptyTrash)
        }
    }
}
