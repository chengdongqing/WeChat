package top.chengdongqing.wechat.feature.chat.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import top.chengdongqing.wechat.core.designsystem.R

/**
 * 群直播屏幕采集服务。
 *
 * MediaProjection 必须运行在 mediaProjection 类型前台服务中。
 * 服务只负责维持前台状态，授权令牌由 WebRTC ScreenCapturerAndroid 唯一消费。
 * Android 14 起同一个授权令牌只能创建一次 MediaProjection，不能再并行创建录屏实例。
 */
class LiveScreenCaptureService : Service() {
    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCapture(intent)
            ACTION_STOP -> stopCapture()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun startCapture(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        if (resultCode != Activity.RESULT_OK) return stopSelf()

        val notification = buildNotification()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else 0
        )
    }

    private fun stopCapture() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_video_filled)
            .setContentTitle("正在共享屏幕")
            .setContentText("群直播正在采集屏幕内容")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "群直播", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "group_live_capture"
        private const val NOTIFICATION_ID = 23041
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val ACTION_START = "wechat.live.capture.START"
        private const val ACTION_STOP = "wechat.live.capture.STOP"

        fun start(context: Context, resultCode: Int) {
            val intent = Intent(context, LiveScreenCaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, LiveScreenCaptureService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
