package com.fluxsync.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.fluxsync.core.desktop.SystemTrayManager
import com.fluxsync.ui.screens.AppRoot
import com.fluxsync.ui.theme.FluxSyncTheme
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

fun main() = application {
    val appScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    var visible by remember { mutableStateOf(true) }
    val windowState = rememberWindowState(width = 1100.dp, height = 720.dp)

    remember {
        ServiceLocator.init(appScope)
        SystemTrayManager.install(
            onOpen = { visible = true },
            onQuit = {
                appScope.cancel()
                exitApplication()
            },
        )
        true
    }

    Window(
        onCloseRequest = {
            visible = false
            SystemTrayManager.minimizeToTray()
        },
        visible = visible,
        state = windowState,
        title = "FluxSync",
        undecorated = false,
    ) {
        FluxSyncTheme {
            AppRoot(ServiceLocator.navigator, ServiceLocator.viewModelStore)
        }
    }
}
