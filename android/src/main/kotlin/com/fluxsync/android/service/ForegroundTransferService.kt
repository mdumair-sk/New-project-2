package com.fluxsync.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.fluxsync.core.model.TransferState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ForegroundTransferService : Service() {
    inner class TransferBinder : Binder() {
        fun service(): ForegroundTransferService = this@ForegroundTransferService
    }

    private val binder = TransferBinder()
    private val _state = MutableStateFlow<TransferState>(TransferState.Idle)
    val state: StateFlow<TransferState> = _state
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun startTransfer(fileName: String, speed: String, percent: Int) {
        acquireWakeLock()
        _state.value = TransferState.Negotiating
        startForeground(NOTIFICATION_ID, buildNotification(fileName, speed, percent))
    }

    fun updateProgress(fileName: String, speed: String, percent: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(fileName, speed, percent))
    }

    fun finishTransfer(state: TransferState) {
        _state.value = state
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "FluxSync transfers", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(fileName: String, speed: String, percent: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(fileName)
            .setContentText(speed)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent.coerceIn(0, 100), false)
            .build()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FluxSync:Transfer").apply {
            acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "fluxsync_transfer"
        private const val NOTIFICATION_ID = 8701
    }
}
