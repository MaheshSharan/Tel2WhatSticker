package com.maheshsharan.tel2what.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.maheshsharan.tel2what.MainActivity
import com.maheshsharan.tel2what.R

class StickerConversionService : Service() {

    companion object {
        private const val CHANNEL_ID = "sticker_conversion_channel"
        private const val NOTIFICATION_ID = 1001
        
        fun start(context: Context, packTitle: String) {
            val intent = Intent(context, StickerConversionService::class.java).apply {
                putExtra("packTitle", packTitle)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateProgress(context: Context, completed: Int, total: Int, packTitle: String) {
            val intent = Intent(context, StickerConversionService::class.java).apply {
                action = "ACTION_UPDATE_PROGRESS"
                putExtra("completed", completed)
                putExtra("total", total)
                putExtra("packTitle", packTitle)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, StickerConversionService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val packTitle = intent?.getStringExtra("packTitle") ?: "Stickers"
        if (intent?.action == "ACTION_UPDATE_PROGRESS") {
            val completed = intent.getIntExtra("completed", 0)
            val total = intent.getIntExtra("total", 0)
            updateNotification(packTitle, completed, total)
        } else {
            val notification = createNotification(packTitle)
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(packTitle: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Converting Stickers")
            .setContentText("Processing '$packTitle' in background")
            .setSmallIcon(R.drawable.ic_splash_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(packTitle: String, completed: Int, total: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = "Progress: $completed / $total completed"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Converting $packTitle")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_splash_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(total, completed, false)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Sticker Conversion Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
