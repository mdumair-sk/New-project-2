package com.fluxsync.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.fluxsync.ui.Strings
import com.fluxsync.ui.theme.FluxColors
import com.fluxsync.ui.theme.Spacing

@Composable
fun AdbWarningBanner(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Icon(Icons.Default.Warning, contentDescription = Strings.AdbWarning, tint = FluxColors.Warning)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = Strings.AdbWarning, color = FluxColors.Warning)
            Text(
                text = Strings.PlatformTools,
                color = FluxColors.Primary,
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://developer.android.com/tools/releases/platform-tools")
                },
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = Strings.Close, tint = FluxColors.OnSurface)
        }
    }
}
