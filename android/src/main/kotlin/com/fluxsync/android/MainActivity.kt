package com.fluxsync.android

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.fluxsync.android.service.ForegroundTransferService
import com.fluxsync.ui.screens.AppRoot
import com.fluxsync.ui.theme.FluxSyncTheme

class MainActivity : ComponentActivity() {
    private var transferService: ForegroundTransferService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            transferService = (service as? ForegroundTransferService.TransferBinder)?.service()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            transferService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ServiceLocator.init(applicationContext, lifecycleScope)
        bindService(Intent(this, ForegroundTransferService::class.java), connection, BIND_AUTO_CREATE)
        requestStorageAccessIfNeeded()
        setContent {
            FluxSyncTheme {
                AppRoot(ServiceLocator.navigator, ServiceLocator.viewModelStore)
            }
        }
    }

    override fun onDestroy() {
        runCatching { unbindService(connection) }
        super.onDestroy()
    }

    private fun requestStorageAccessIfNeeded() {
        if (Environment.isExternalStorageManager()) return
        val uri = Uri.parse("package:$packageName")
        startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri))
    }
}
