package top.chengdongqing.wechat.data.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.features.call.ui.CallActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知管理器
 */
@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {
        // 通知渠道
        const val MESSAGE_CHANNEL_ID = "message_channel"
        const val CALL_CHANNEL_ID = "call_channel"

        // 通知 ID
        const val FRIEND_REQUEST_NOTIFICATION_ID = 2001
        const val CALL_NOTIFICATION_ID = 2002
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // 获取系统默认通知音的 URI
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // 消息通知通道
        val messageChannel = NotificationChannel(
            MESSAGE_CHANNEL_ID,
            "新消息",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "收到新消息时使用的通知类别"
            setSound(alarmSound, Notification.AUDIO_ATTRIBUTES_DEFAULT)
            setShowBadge(true)
            enableLights(true)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC // 锁屏也显示
        }

        // 通话通知渠道
        val callChannel = NotificationChannel(
            CALL_CHANNEL_ID,
            "音视频通话邀请通知",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "收到音视频通话邀请时使用的通知类别"
            setSound(null, null) // 避免触发系统铃声，用自定义铃声
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC  // 锁屏也显示
        }

        notificationManager.createNotificationChannel(messageChannel)
        notificationManager.createNotificationChannel(callChannel)
    }

    /**
     * 显示好友请求通知
     */
    fun showFriendRequestNotification(
        title: String,
        content: String,
        notificationId: Int = FRIEND_REQUEST_NOTIFICATION_ID
    ) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = "wechat://contacts/new_friends".toUri()
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.img_logo)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * 显示消息通知
     */
    fun showMessageNotification(
        sessionId: String,
        title: String,
        content: String,
        notificationId: Int
    ) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = "wechat://chat/$sessionId".toUri()
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.img_logo)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * 显示来电通知
     *
     * 使用 fullScreenIntent 在锁屏/息屏时直接唤起通话界面。
     * CATEGORY_CALL 使系统对来电通知给予最高优先级展示。
     */
    @SuppressLint("FullScreenIntentPolicy")
    fun showIncomingNotification(
        peerName: String,
        isVideo: Boolean,
        notificationId: Int = CALL_NOTIFICATION_ID
    ) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, CallActivity::class.java).apply {
                setPackage(context.packageName)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val typeText = if (isVideo) "视频通话" else "语音通话"
        val notification = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.img_logo)
            .setContentTitle(peerName)
            .setContentText("$typeText 来电")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * 显示通话进行中通知（呼出中 / 连接中 / 通话中），点击可返回通话界面
     */
    fun showOngoingNotification(text: String) {
        val intent = PendingIntent.getActivity(
            context,
            CALL_NOTIFICATION_ID,
            Intent(context, CallActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.img_logo)
            .setContentTitle(text)
            .setContentIntent(intent)
            .setOngoing(true)
            .build()

        notificationManager.notify(CALL_NOTIFICATION_ID, notification)
    }

    /**
     * 取消通知
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}