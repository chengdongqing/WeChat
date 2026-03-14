package top.chengdongqing.wechat.data.network.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.core.media.VibratorHelper
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.model.toPreviewText
import top.chengdongqing.wechat.data.network.connection.ChatTransportManager
import top.chengdongqing.wechat.data.network.connection.ConnectionMode
import top.chengdongqing.wechat.data.network.service.modules.BLEModule
import top.chengdongqing.wechat.data.network.service.modules.BluetoothChatModule
import top.chengdongqing.wechat.data.network.service.modules.CallModule
import top.chengdongqing.wechat.data.network.service.modules.FriendRequestEvent
import top.chengdongqing.wechat.data.network.service.modules.WiFiDirectChatModule
import top.chengdongqing.wechat.data.network.service.modules.WiFiLanChatModule
import top.chengdongqing.wechat.data.notification.NotificationHelper
import top.chengdongqing.wechat.data.session.ActiveSessionManager
import top.chengdongqing.wechat.features.chat.data.mapper.toMessageType
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import top.chengdongqing.wechat.features.settings.domain.model.NotificationDisplay
import top.chengdongqing.wechat.features.settings.domain.model.NotificationSound
import top.chengdongqing.wechat.features.settings.domain.model.toUri
import top.chengdongqing.wechat.features.settings.domain.repository.ConnectionSettingsRepository
import top.chengdongqing.wechat.features.settings.domain.repository.NotificationSettingsRepository
import javax.inject.Inject

/**
 * 网络前台服务
 *
 * 以前台服务形式长期运行，统一管理多个子模块：
 * - [BLEModule]：蓝牙设备发现与好友添加
 * - [WiFiLanChatModule]：TCP 消息收发
 * - [CallModule]：音视频通话管理
 *
 * 启动流程：前台通知 → 读取个人资料 → 各模块按序启动 → 订阅事件流
 */
@AndroidEntryPoint
class NetworkService : Service() {

    @Inject
    lateinit var bleModule: BLEModule

    @Inject
    lateinit var wifiLanChatModule: WiFiLanChatModule

    @Inject
    lateinit var bluetoothChatModule: BluetoothChatModule

    @Inject
    lateinit var wifiDirectChatModule: WiFiDirectChatModule

    @Inject
    lateinit var connectionSettingsRepository: ConnectionSettingsRepository

    @Inject
    lateinit var chatTransportManager: ChatTransportManager

    @Inject
    lateinit var callModule: CallModule

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var contactRepository: ContactRepository

    @Inject
    lateinit var chatSessionDao: ChatSessionDao

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var vibratorHelper: VibratorHelper

    @Inject
    lateinit var notificationRepository: NotificationSettingsRepository

    @Inject
    lateinit var activeSessionManager: ActiveSessionManager

    @Inject
    @IoScope
    lateinit var scope: CoroutineScope

    @Inject
    @ApplicationContext
    lateinit var context: Context

    companion object {
        private const val TAG = "P2PService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "p2p_service_channel"

        const val ACTION_START_CONNECT = "action_start_connect" // 登录成功后调用
        const val ACTION_STOP_CONNECT = "action_stop_connect"   // 退出登录调用
        const val ACTION_RETRY_BLE = "action_retry_ble"         // 权限授予后调用
    }

    // ==================== 生命周期 ====================

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CONNECT -> {
                // 注册通知
                createNotificationChannel()
                startForegroundService()
                // 启动各个子模块
                scope.launch { initializeModules() }
            }

            ACTION_RETRY_BLE -> {
                bleModule.start(scope)
            }

            ACTION_STOP_CONNECT -> {
                scope.launch { stopAllModules() }
            }
        }

        return START_STICKY // 被系统杀死后自动重启，保持消息收发能力
    }

    override fun onDestroy() {
        super.onDestroy()

        scope.launch {
            stopAllModules()
            Log.d(TAG, "P2P 服务已停止")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ==================== 初始化 ====================

    /**
     * 按序启动各子模块并订阅事件流
     *
     * 依赖个人资料（userId）才能建立连接，资料不存在时提前退出。
     */
    private suspend fun initializeModules() {
        try {
            val myProfile = profileRepository.getProfile() ?: return
            val connectionMode = connectionSettingsRepository.connectionMode.first()

            bleModule.start(scope)

            // 根据连接模式启动对应聊天模块
            when (connectionMode) {
                ConnectionMode.WiFiLan -> wifiLanChatModule.start(myProfile.id, scope)
                ConnectionMode.Bluetooth -> bluetoothChatModule.start()
                ConnectionMode.WiFiDirect -> wifiDirectChatModule.prepare()
            }

            // 启动通话模块（视频/语音通话）
            callModule.start(myProfile.id, scope)

            // 监听好友请求事件
            scope.launch { observeFriendRequestEvents() }
            // 监听新消息
            scope.launch { observeIncomingMessages() }
            // 监听连接模式切换，重启聊天模块
            scope.launch { observeConnectionModeChanges(myProfile.id) }
            // 监听连接模式切换，动态管理连接
            scope.launch { chatTransportManager.observeMode(connectionSettingsRepository.connectionMode) }
        } catch (e: Exception) {
            Log.e(TAG, "初始化模块失败", e)
        }
    }

    private suspend fun stopAllModules() {
        bleModule.stop()
        wifiLanChatModule.stop()
        bluetoothChatModule.stop()
        wifiDirectChatModule.stop()
        callModule.stop()
    }

    // ==================== 事件订阅 ====================

    private suspend fun observeConnectionModeChanges(myUserId: String) {
        Log.d(TAG, "开始监听模式变化")
        connectionSettingsRepository.connectionMode
            .drop(1)  // 跳过初始值，只响应后续变化
            .collect { newMode ->
                try {
                    Log.d(TAG, "收到模式变化: $newMode")

                    // 断开现有连接
                    chatTransportManager.disconnectAll()

                    // 停止所有聊天模块
                    wifiLanChatModule.stop()
                    bluetoothChatModule.stop()

                    Log.d(TAG, "开始重启新模式: $newMode")

                    // 启动新模式对应的模块
                    when (newMode) {
                        ConnectionMode.WiFiLan -> wifiLanChatModule.start(myUserId, scope)
                        ConnectionMode.Bluetooth -> bluetoothChatModule.start()
                        ConnectionMode.WiFiDirect -> {}
                    }

                    Log.d(TAG, "模式已切换")
                } catch (e: Exception) {
                    Log.e(TAG, "模式切换异常", e)
                }
            }
    }

    /**
     * 监听 BLE 好友请求事件，触发对应通知
     */
    private suspend fun observeFriendRequestEvents() {
        bleModule.friendRequestEvents.collect { event ->
            handleFriendRequestEvent(event)
        }
    }

    /**
     * 监听新消息
     */
    private suspend fun observeIncomingMessages() {
        wifiLanChatModule.incomingMessageFlow.collect { message ->
            handleNewMessage(message)
        }
    }

    // ==================== 事件处理 ====================

    /**
     * 处理好友请求事件，按类型展示不同通知文案
     */
    private fun handleFriendRequestEvent(event: FriendRequestEvent) {
        when (event) {
            is FriendRequestEvent.NewRequest -> notificationHelper.showFriendRequestNotification(
                title = event.nickname,
                content = context.getString(R.string.contact_notification_request_content)
            )

            is FriendRequestEvent.RequestAccepted -> notificationHelper.showFriendRequestNotification(
                title = context.getString(R.string.contact_notification_accepted_title),
                content = event.message
            )

            is FriendRequestEvent.AutoAdded -> notificationHelper.showFriendRequestNotification(
                title = context.getString(R.string.contact_notification_auto_added_title),
                content = context.getString(
                    R.string.contact_notification_auto_added_content,
                    event.nickname
                )
            )

            else -> {}
        }
    }

    /**
     * 处理新消息通知
     */
    private suspend fun handleNewMessage(message: ChatMessage) {
        val soundEnabled = inChatSoundEnabled()
        val vibrationEnabled = inChatVibrationEnabled()

        if (soundEnabled && vibrationEnabled) {
            val contact = contactRepository.getContact(message.senderId)
            val sender = contact?.displayName
                ?: context.getString(R.string.chat_notification_contact_unknown)
            val unreadCount = chatSessionDao.getById(message.senderId)?.unreadCount ?: 0
            // 获取消息内容
            val content = resolveContent(message)

            // 构建通知内容
            val (title, text) = resolveNotificationText(sender, content, unreadCount)
            // 获取头像
            val avatarBitmap = contact?.avatarPath?.let { BitmapFactory.decodeFile(it) }

            // 显示通知
            notificationHelper.showMessageNotification(
                sessionId = message.sessionId,
                title = title,
                content = text,
                notificationId = message.sessionId.hashCode(),
                avatarBitmap = avatarBitmap
            )
        }

        if (soundEnabled) {
            val soundUri = notificationSound().toUri(context)
            RingtoneManager.getRingtone(context, soundUri).play()
        }
        if (vibrationEnabled) {
            vibratorHelper.vibrate(longArrayOf(0, 250, 250, 250))
        }
    }

    private fun resolveContent(message: ChatMessage): String {
        val textContent = (message.content as? MessageContent.Text)?.text ?: ""
        return message.content.toMessageType().toPreviewText(context, textContent)
    }

    private suspend fun resolveNotificationText(
        sender: String,
        content: String,
        unreadCount: Int
    ): Pair<String?, String> {
        val prefix = if (unreadCount > 1) "[${unreadCount}条] " else ""
        return when (notificationDisplay()) {
            NotificationDisplay.HiddenAll -> Pair(
                null,
                prefix + context.getString(R.string.chat_notification_hidden)
            )

            NotificationDisplay.SenderOnly -> Pair(
                sender,
                prefix + context.getString(R.string.chat_notification_sender_only)
            )

            NotificationDisplay.SenderAndContent -> Pair(
                sender,
                prefix + content
            )
        }
    }

    /**
     * 创建前台服务通知渠道（其他业务通知渠道由 NotificationHelper 管理）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "P2P通信服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持设备发现和消息收发功能运行"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 启动前台服务，展示常驻通知防止被系统回收
     */
    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("P2P服务运行中")
            .setSmallIcon(R.drawable.img_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private suspend fun notificationDisplay(): NotificationDisplay =
        notificationRepository.notificationDisplay.first()

    private suspend fun inChatSoundEnabled(): Boolean =
        !activeSessionManager.inChat || notificationRepository.inChatSoundEnabled.first()

    private suspend fun inChatVibrationEnabled(): Boolean =
        !activeSessionManager.inChat || notificationRepository.inChatVibrationEnabled.first()

    private suspend fun notificationSound(): NotificationSound =
        notificationRepository.notificationSound.first()
}

fun Context.createNetworkServiceIntent(action: String): Intent {
    return Intent(this, NetworkService::class.java).apply {
        this.action = action
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}