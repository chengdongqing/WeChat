package top.chengdongqing.wechat.service.call

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import top.chengdongqing.wechat.core.network.model.NotificationChannelConfig
import top.chengdongqing.wechat.core.network.model.NotificationId
import top.chengdongqing.wechat.feature.call.service.CallActionReceiver
import top.chengdongqing.wechat.feature.call.ui.CallActivity
import top.chengdongqing.wechat.core.designsystem.R as DesignR

/**
 * 通话通知必须是前台服务的主通知，Android 14+ 才允许使用 CallStyle。
 */
class CallNotificationService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = when (intent?.action) {
            ACTION_INCOMING -> createIncomingNotification(intent)
            ACTION_ONGOING -> createOngoingNotification(intent)
            else -> return START_NOT_STICKY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationId.Call.id,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(NotificationId.Call.id, notification)
        }
        return START_NOT_STICKY
    }

    @SuppressLint("FullScreenIntentPolicy")
    private fun createIncomingNotification(intent: Intent): Notification {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        val contentIntent = callActivityIntent()
        return NotificationCompat.Builder(this, NotificationChannelConfig.Call.id)
            .setSmallIcon(DesignR.drawable.img_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSound(null)
            .setVibrate(null)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    Person.Builder().setName(title.ifBlank { "微信用户" }).setImportant(true).build(),
                    callAction(3101, CallActionReceiver.ACTION_DECLINE),
                    callAction(3102, CallActionReceiver.ACTION_ACCEPT)
                ).setIsVideo(intent.getBooleanExtra(EXTRA_IS_VIDEO, false))
            )
            .build()
    }

    private fun createOngoingNotification(intent: Intent): Notification {
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        val peerName = intent.getStringExtra(EXTRA_PEER_NAME).orEmpty()
        val muted = intent.getBooleanExtra(EXTRA_IS_MUTED, false)
        return NotificationCompat.Builder(this, NotificationChannelConfig.OngoingCall.id)
            .setSmallIcon(DesignR.drawable.img_logo)
            .setContentTitle(text)
            .setContentText(if (muted) "麦克风已关闭" else "正在通话")
            .setContentIntent(callActivityIntent())
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setWhen(
                System.currentTimeMillis() -
                    intent.getLongExtra(EXTRA_DURATION_SECONDS, 0L) * 1000
            )
            .setUsesChronometer(intent.getBooleanExtra(EXTRA_SHOW_CHRONOMETER, false))
            .setChronometerCountDown(false)
            .setStyle(
                NotificationCompat.CallStyle.forOngoingCall(
                    Person.Builder()
                        .setName(peerName.ifBlank { "微信用户" })
                        .setImportant(true)
                        .build(),
                    callAction(3103, CallActionReceiver.ACTION_HANGUP)
                ).setIsVideo(intent.getBooleanExtra(EXTRA_IS_VIDEO, false))
            )
            .addAction(
                0,
                if (muted) "取消静音" else "静音",
                callAction(3104, CallActionReceiver.ACTION_TOGGLE_MUTE)
            )
            .build()
    }

    private fun callActivityIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        NotificationId.Call.id,
        Intent(this, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun callAction(requestCode: Int, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            requestCode,
            Intent(this, CallActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        private const val ACTION_INCOMING = "call_notification.incoming"
        private const val ACTION_ONGOING = "call_notification.ongoing"
        private const val ACTION_STOP = "call_notification.stop"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_TEXT = "text"
        private const val EXTRA_PEER_NAME = "peer_name"
        private const val EXTRA_DURATION_SECONDS = "duration_seconds"
        private const val EXTRA_SHOW_CHRONOMETER = "show_chronometer"
        private const val EXTRA_IS_MUTED = "is_muted"
        private const val EXTRA_IS_VIDEO = "is_video"

        fun incoming(context: android.content.Context, title: String, text: String, isVideo: Boolean) {
            start(
                context,
                Intent(context, CallNotificationService::class.java)
                    .setAction(ACTION_INCOMING)
                    .putExtra(EXTRA_TITLE, title)
                    .putExtra(EXTRA_TEXT, text)
                    .putExtra(EXTRA_IS_VIDEO, isVideo)
            )
        }

        fun ongoing(
            context: android.content.Context,
            text: String,
            peerName: String,
            durationSeconds: Long,
            showChronometer: Boolean,
            isMuted: Boolean,
            isVideo: Boolean
        ) {
            start(
                context,
                Intent(context, CallNotificationService::class.java)
                    .setAction(ACTION_ONGOING)
                    .putExtra(EXTRA_TEXT, text)
                    .putExtra(EXTRA_PEER_NAME, peerName)
                    .putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                    .putExtra(EXTRA_SHOW_CHRONOMETER, showChronometer)
                    .putExtra(EXTRA_IS_MUTED, isMuted)
                    .putExtra(EXTRA_IS_VIDEO, isVideo)
            )
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, CallNotificationService::class.java))
        }

        private fun start(context: android.content.Context, intent: Intent) {
            context.startForegroundService(intent)
        }
    }
}
