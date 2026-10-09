package com.astraflow.MysticSky.keepalive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.astraflow.MysticSky.R
import com.astraflow.MysticSky.capability.link.LinkHub
import com.astraflow.MysticSky.capability.lyricprovider.StandaloneLyric
import com.astraflow.MysticSky.settings.ModulePrefs

class KeepAliveService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    private val lyricBeat = object : Runnable {
        override fun run() {
            runCatching {
                val enabled = ModulePrefs.isStandaloneLyric(ModulePrefs.of(this@KeepAliveService))
                StandaloneLyric.tick(this@KeepAliveService, enabled)
            }
            handler.postDelayed(this, if (StandaloneLyric.lastActive()) LYRIC_BEAT_MS else LYRIC_IDLE_MS)
        }
    }

    private val beat = object : Runnable {
        override fun run() {

            runCatching { LinkHub.warmUp(this@KeepAliveService) }
            handler.postDelayed(this, BEAT_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        running = true
        createChannel()
        runCatching { startForeground(NOTIFY_ID, buildNotification("链接助手就绪，复制链接即可上岛")) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.removeCallbacks(beat)
        handler.post(beat)
        handler.removeCallbacks(lyricBeat)
        handler.postDelayed(lyricBeat, 2500L)

        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(beat)
        handler.removeCallbacks(lyricBeat)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.keepalive_channel),

            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
            description = getString(R.string.keepalive_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CHANNEL_ID)
        else
            @Suppress("DEPRECATION") Notification.Builder(this)
        return builder
            .setContentTitle(getString(R.string.keepalive_notify_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_star)
            .setOngoing(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "mitian_keepalive"
        private const val NOTIFY_ID = 41001
        private const val BEAT_MS = 60_000L
        private const val LYRIC_BEAT_MS = 3_000L

        private const val LYRIC_IDLE_MS = 20_000L

        @Volatile
        var running: Boolean = false
            private set

        fun start(context: Context) {
            runCatching {
                context.startForegroundService(Intent(context, KeepAliveService::class.java))
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, KeepAliveService::class.java)) }
        }
    }
}
