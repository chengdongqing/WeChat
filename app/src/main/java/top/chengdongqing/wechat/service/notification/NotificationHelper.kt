package top.chengdongqing.wechat.service.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import top.chengdongqing.wechat.MainActivity
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.common.navigation.ChatRoute
import top.chengdongqing.wechat.core.common.navigation.ContactsRoute
import top.chengdongqing.wechat.core.network.model.NotificationChannelConfig
import top.chengdongqing.wechat.core.network.model.NotificationId
import top.chengdongqing.wechat.feature.call.ui.CallActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知管理器
 */
@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createNotificationChannels()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannels() {
        // 消息通知通道
        NotificationChannel(
            NotificationChannelConfig.Message.id,
            NotificationChannelConfig.Message.title,
            NotificationChannelConfig.Message.importance
        ).apply {
            setSound(null, null)
            enableVibration(false)
            description = NotificationChannelConfig.Message.description
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC // 锁屏也显示
        }.also {
            notificationManager.createNotificationChannel(it)
        }

        // 通话通知渠道
        NotificationChannel(
            NotificationChannelConfig.Call.id,
            NotificationChannelConfig.Call.title,
            NotificationChannelConfig.Call.importance
        ).apply {
            setSound(null, null)
            enableVibration(false)
            description = NotificationChannelConfig.Call.description
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC  // 锁屏也显示
        }.also {
            notificationManager.createNotificationChannel(it)
        }
    }

    /**
     * 显示好友请求通知
     */
    fun showFriendNotification(
        title: String,
        content: String,
        contactId: String? = null,
        notificationId: Int = NotificationId.FriendRequest.id
    ) {
        // 构建点击通知跳转的 Intent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

            if (contactId != null) {
                // 加好友成功的通知跳转到聊天详情
                ChatRoute.ChatSession.createRoute(contactId)
            } else {
                // 其余跳转到新的朋友
                ContactsRoute.NewFriends.route
            }.also { targetRoute ->
                putExtra(MainActivity.EXTRA_ROUTE, targetRoute)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知
        val notification = NotificationCompat.Builder(context, NotificationChannelConfig.Message.id)
            .setSmallIcon(R.drawable.img_logo)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        // 显示通知
        notificationManager.notify(notificationId, notification)
    }

    /**
     * 显示消息通知
     */
    fun showMessageNotification(
        sessionId: String,
        title: String?,
        content: String,
        notificationId: Int,
        avatarBitmap: Bitmap?
    ) {
        // 构建点击通知跳转的 Intent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_ROUTE, ChatRoute.ChatSession.createRoute(sessionId))
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知
        val notification = NotificationCompat.Builder(context, NotificationChannelConfig.Message.id)
            .setSmallIcon(R.drawable.img_logo) // app logo
            .setLargeIcon(avatarBitmap) // 头像
            .setSound(null)
            .setVibrate(null)
            .setContentTitle(title) // 标题
            .setContentText(content) // 内容
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 优先级
            .setCategory(NotificationCompat.CATEGORY_MESSAGE) // 帮助系统进行通知分类
            .setContentIntent(pendingIntent) // 支持点击跳转
            .setAutoCancel(true) // 自动消失
            .build()

        // 显示通知
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
        title: String,
        text: String,
        notificationId: Int = NotificationId.Call.id
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
        val notification = NotificationCompat.Builder(context, NotificationChannelConfig.Call.id)
            .setSmallIcon(R.drawable.img_logo)
            .setSound(null)
            .setVibrate(null)
            .setContentTitle(title)
            .setContentText(text)
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
    fun showOngoingNotification(
        text: String,
        notificationId: Int = NotificationId.Call.id
    ) {
        val intent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, CallActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationChannelConfig.Call.id)
            .setSmallIcon(R.drawable.img_logo)
            .setSound(null)
            .setVibrate(null)
            .setContentTitle(text)
            .setContentIntent(intent)
            .setOngoing(true)
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