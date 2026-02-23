package top.chengdongqing.wechat.data.network.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.network.service.modules.BLEModule
import top.chengdongqing.wechat.data.network.service.modules.CallModule
import top.chengdongqing.wechat.data.network.service.modules.ChatModule
import top.chengdongqing.wechat.data.network.service.modules.FriendRequestEvent
import top.chengdongqing.wechat.data.notification.NotificationHelper
import top.chengdongqing.wechat.data.session.ActiveSessionManager
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.toPreviewText
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * 网络后台服务
 *
 * 以前台服务形式长期运行，统一管理多个子模块：
 * - [BLEModule]：蓝牙设备发现与好友添加
 * - [ChatModule]：TCP 消息收发
 * - [CallModule]：音视频通话管理
 *
 * 启动流程：前台通知 → 读取个人资料 → 各模块按序启动 → 订阅事件流
 */
@AndroidEntryPoint
class NetworkService : Service() {

    @Inject
    lateinit var bleModule: BLEModule

    @Inject
    lateinit var chatModule: ChatModule

    @Inject
    lateinit var callModule: CallModule

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var contactRepository: ContactRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var activeSessionManager: ActiveSessionManager

    @Inject
    @IoScope
    lateinit var scope: CoroutineScope

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
                stopAllModules()
            }
        }

        return START_STICKY // 被系统杀死后自动重启，保持消息收发能力
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllModules()
        Log.d(TAG, "P2P 服务已停止")
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
            val myProfile = profileRepository.getCurrentProfileSnapshot() ?: run {
                Log.w(TAG, "未找到个人资料，服务启动失败")
                return
            }

            // 启动 BLE 模块（好友添加）
            bleModule.start(scope)
            // 启动聊天模块（消息收发）
            chatModule.start(myProfile.id, scope)
            // 启动通话模块（视频/语音通话）
            callModule.start(myProfile.id, scope)

            // 监听好友请求事件
            scope.launch { observeFriendRequestEvents() }
            // 监听新消息
            scope.launch { observeIncomingMessages() }

            Log.d(TAG, "所有模块已启动")
        } catch (e: Exception) {
            Log.e(TAG, "初始化模块失败", e)
        }
    }

    private fun stopAllModules() {
        bleModule.stop()
        chatModule.stop()
        callModule.stop()
    }

    // ==================== 事件订阅 ====================

    /**
     * 监听 BLE 好友请求事件，触发对应通知
     */
    private suspend fun observeFriendRequestEvents() {
        bleModule.friendRequestEvents.collect { event ->
            handleFriendRequestEvent(event)
        }
    }

    /**
     * 监听新消息，当前正在查看该会话时不发通知
     * 自己发送的消息也不通知（对方 ACK 触发的流转）
     */
    private suspend fun observeIncomingMessages() {
        chatModule.incomingMessageFlow.collect { message ->
            if (!activeSessionManager.isActive(message.sessionId)) {
                handleNewMessage(message)
            }
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
                content = "请求添加你为朋友"
            )

            is FriendRequestEvent.RequestAccepted -> notificationHelper.showFriendRequestNotification(
                title = "好友申请",
                content = event.message
            )

            is FriendRequestEvent.AutoAdded -> notificationHelper.showFriendRequestNotification(
                title = "新的朋友",
                content = "你已添加了${event.nickname}，现在可以开始聊天了"
            )

            else -> {}
        }
    }

    /**
     * 处理新消息通知
     *
     * 发件人优先取备注名，查不到联系人时兜底显示"新消息"。
     */
    private suspend fun handleNewMessage(message: ChatMessage) {
        if (message.isFromMe) return // 自己发送的消息不通知

        // 查询联系人昵称
        val contact = contactRepository.getContactById(message.senderId)
        val senderName = contact?.displayName ?: "新消息"
        // 获取内容预览信息
        val previewText = message.content.toPreviewText()

        notificationHelper.showMessageNotification(
            sessionId = message.sessionId,
            title = senderName,
            content = previewText,
            notificationId = message.id.hashCode()
        )
    }

    // ==================== 前台通知 ====================

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
}

fun Context.createNetworkServiceIntent(action: String): Intent {
    return Intent(this, NetworkService::class.java).apply {
        this.action = action
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}