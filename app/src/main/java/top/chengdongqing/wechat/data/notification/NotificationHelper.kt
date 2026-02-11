package top.chengdongqing.wechat.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import top.chengdongqing.wechat.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统一通知管理器
 */
@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private companion object {
        // 通知渠道
        const val FRIEND_REQUEST_CHANNEL_ID = "friend_request_channel"
        const val MESSAGE_CHANNEL_ID = "message_channel"

        // 通知 ID
        const val FRIEND_REQUEST_NOTIFICATION_ID = 2001
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 好友请求通道
            val friendRequestChannel = NotificationChannel(
                FRIEND_REQUEST_CHANNEL_ID,
                "好友请求",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "新的好友请求通知"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
            }

            // 消息通知通道
            val messageChannel = NotificationChannel(
                MESSAGE_CHANNEL_ID,
                "新消息",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "接收新消息通知"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(friendRequestChannel)
            notificationManager.createNotificationChannel(messageChannel)
        }
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FRIEND_REQUEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_add_friends_filled)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * 取消通知
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}