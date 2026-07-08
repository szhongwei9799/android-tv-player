package com.multimediaplayer.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.multimediaplayer.R
import com.multimediaplayer.utils.AppLogger

class WebService : Service() {
    private var webServer: WebServer? = null
    
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        AppLogger.i("WebService", "Service created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startServer()
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }
    
    private fun startServer() {
        if (webServer == null) {
            webServer = WebServer(this, PORT)
            webServer?.start()
        }
    }
    
    private fun stopServer() {
        webServer?.stop()
        webServer = null
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Web服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "多媒体展示系统Web服务"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("多媒体展示系统")
            .setContentText("Web服务运行中")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    companion object {
        private const val CHANNEL_ID = "web_service_channel"
        private const val NOTIFICATION_ID = 1
        const val PORT = 8080
    }
}
