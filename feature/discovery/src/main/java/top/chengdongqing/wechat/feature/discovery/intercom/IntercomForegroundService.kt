package top.chengdongqing.wechat.feature.discovery.intercom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.network.audio.IntercomAudioEngine
import javax.inject.Inject

/** Keeps channel discovery and playback alive while the app is backgrounded. */
@AndroidEntryPoint
class IntercomForegroundService : Service() {
    @Inject
    lateinit var discovery: IntercomLanDiscovery
    @Inject
    lateinit var audioEngine: IntercomAudioEngine

    private var activeChannel: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_LEAVE) {
            stopSelf()
            return START_NOT_STICKY
        }
        val channel = intent?.getStringExtra(EXTRA_CHANNEL)
            ?.takeIf(String::isNotBlank)
            ?: activeChannel
            ?: return START_NOT_STICKY
        activeChannel = channel
        val mode = ConnectionMode.fromName(
            intent?.getStringExtra(EXTRA_CONNECTION_MODE)
        )
        discovery.start(mode)
        discovery.join(channel)
        audioEngine.start(channel, mode)
        startForeground(NOTIFICATION_ID, createNotification(channel))
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        audioEngine.stop()
        discovery.leave()
        activeChannel = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(channel: String): Notification {
        val contentIntent = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val leaveIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, IntercomForegroundService::class.java).setAction(ACTION_LEAVE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic_filled)
            .setContentTitle("正在收听频道 #$channel")
            .setContentText("语音对讲保持在线 · 点击返回应用")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_close_outlined, "退出频道", leaveIntent)
            .build()
    }

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "语音对讲",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示当前正在收听的局域网对讲频道"
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    companion object {
        const val ACTION_JOIN =
            "top.chengdongqing.wechat.feature.discovery.intercom.action.JOIN"
        const val ACTION_LEAVE =
            "top.chengdongqing.wechat.feature.discovery.intercom.action.LEAVE"
        const val EXTRA_CHANNEL = "channel"
        const val EXTRA_CONNECTION_MODE = "connection_mode"
        private const val NOTIFICATION_CHANNEL_ID = "intercom_active_channel"
        private const val NOTIFICATION_ID = 52_142
    }
}
