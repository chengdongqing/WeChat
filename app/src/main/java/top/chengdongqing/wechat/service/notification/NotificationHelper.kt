package top.chengdongqing.wechat.service.notification

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.MainActivity
import top.chengdongqing.wechat.core.navigation.NavigationKey
import top.chengdongqing.wechat.core.network.model.NotificationChannelConfig
import top.chengdongqing.wechat.core.network.model.NotificationId
import top.chengdongqing.wechat.service.call.CallNotificationService
import javax.inject.Inject
import javax.inject.Singleton
import top.chengdongqing.wechat.core.designsystem.R as DesignR

/**
 * 通知管理器
 */
@Singleton
class NotificationHelper @Inject constructor(
    private val json: Json,
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

        NotificationChannel(
            NotificationChannelConfig.OngoingCall.id,
            NotificationChannelConfig.OngoingCall.title,
            NotificationChannelConfig.OngoingCall.importance
        ).apply {
            setSound(null, null)
            enableVibration(false)
            vibrationPattern = null
            description = NotificationChannelConfig.OngoingCall.description
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }.also(notificationManager::createNotificationChannel)
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
                NavigationKey.ChatSession(contactId)
            } else {
                // 其余跳转到新的朋友
                NavigationKey.NewFriends
            }.also { targetNav ->
                val navJson = json.encodeToString<NavigationKey>(targetNav)
                putExtra(MainActivity.EXTRA_NAV, navJson)
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
            .setSmallIcon(DesignR.drawable.img_logo)
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

            val targetNav = NavigationKey.ChatSession(sessionId)
            val navJson = json.encodeToString<NavigationKey>(targetNav)
            putExtra(MainActivity.EXTRA_NAV, navJson)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知
        val notification = NotificationCompat.Builder(context, NotificationChannelConfig.Message.id)
            .setSmallIcon(DesignR.drawable.img_logo) // app logo
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
        isVideo: Boolean = false
    ) {
        wakeScreenForIncomingCall()
        CallNotificationService.incoming(context, title, text, isVideo)
        return
    }

    /**
     * 显示通话进行中通知（呼出中 / 连接中 / 通话中），点击可返回通话界面
     */
    fun showOngoingNotification(
        text: String,
        peerName: String = text,
        durationSeconds: Long = 0,
        showChronometer: Boolean = false,
        isMuted: Boolean = false,
        isVideo: Boolean = false
    ) {
        CallNotificationService.ongoing(
            context = context,
            text = text,
            peerName = peerName,
            durationSeconds = durationSeconds,
            showChronometer = showChronometer,
            isMuted = isMuted,
            isVideo = isVideo
        )
    }

    /**
     * 取消通知
     */
    fun cancelNotification(notificationId: Int) {
        if (notificationId == NotificationId.Call.id) {
            CallNotificationService.stop(context)
        }
        notificationManager.cancel(notificationId)
    }

    /**
     * 某些厂商在息屏状态不会仅因 fullScreenIntent 点亮屏幕。短暂唤醒屏幕后，
     * 系统仍然负责决定是否展示全屏来电页，不会长期阻止设备重新休眠。
     */
    @Suppress("DEPRECATION")
    private fun wakeScreenForIncomingCall() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (powerManager.isInteractive && !keyguardManager.isKeyguardLocked) return

        runCatching {
            powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "${context.packageName}:incoming-call"
            ).apply {
                setReferenceCounted(false)
                acquire(INCOMING_CALL_WAKE_LOCK_TIMEOUT_MS)
            }
        }.onFailure {
            Log.w(TAG, "无法点亮来电屏幕", it)
        }
    }

    private companion object {
        const val TAG = "NotificationHelper"
        const val INCOMING_CALL_WAKE_LOCK_TIMEOUT_MS = 10_000L
    }
}
