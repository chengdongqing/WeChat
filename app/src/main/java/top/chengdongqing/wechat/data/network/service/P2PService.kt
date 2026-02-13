package top.chengdongqing.wechat.data.network.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.network.service.modules.BLEModule
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
 * P2P 通信服务
 *
 * 统一管理：
 * 1. BLE 模块 - 好友添加
 * 2. Chat 模块 - 消息收发
 */
@AndroidEntryPoint
class P2PService : Service() {

    @Inject
    lateinit var bleModule: BLEModule

    @Inject
    lateinit var chatModule: ChatModule

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var contactRepository: ContactRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var activeSessionManager: ActiveSessionManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private companion object {
        const val TAG = "P2PService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "p2p_service_channel"
    }

    // ==================== 生命周期 ====================

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "P2P 服务启动")

        createForegroundServiceChannel()  // ✅ 只创建前台服务通道
        startForegroundService()

        serviceScope.launch {
            initializeServices()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllServices()
        serviceScope.cancel()
        Log.d(TAG, "P2P 服务已停止")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ==================== 初始化 ====================

    private suspend fun initializeServices() {
        try {
            val myProfile = profileRepository.getCurrentProfile().first()

            if (myProfile == null) {
                Log.w(TAG, "未找到个人资料，服务启动失败")
                return
            }

            // ✅ 启动 BLE 模块（好友添加）
            bleModule.start(serviceScope)

            // ✅ 启动聊天模块（消息收发）
            chatModule.start(myProfile.id, serviceScope)

            // ✅ 监听好友请求事件
            serviceScope.launch {
                bleModule.friendRequestEvents.collect { event ->
                    handleFriendRequestEvent(event)
                }
            }

            // ✅ 监听新消息
            serviceScope.launch {
                chatModule.incomingMessageFlow.collect { message ->
                    // 正在查看该会话，不发通知
                    if (!activeSessionManager.isActive(message.sessionId)) {
                        handleNewMessage(message)
                    }
                }
            }

            Log.d(TAG, "✅ 所有模块已启动")

        } catch (e: Exception) {
            Log.e(TAG, "初始化服务失败", e)
        }
    }

    private fun stopAllServices() {
        bleModule.stop()
        chatModule.stop()
    }

    // ==================== 事件处理 ====================

    /**
     * ✅ 处理好友请求事件
     */
    private fun handleFriendRequestEvent(event: FriendRequestEvent) {
        when (event) {
            is FriendRequestEvent.NewRequest -> {
                notificationHelper.showFriendRequestNotification(
                    title = event.nickname,
                    content = "请求添加你为朋友"
                )
            }

            is FriendRequestEvent.RequestAccepted -> {
                notificationHelper.showFriendRequestNotification(
                    title = "好友申请",
                    content = event.message
                )
            }

            is FriendRequestEvent.AutoAdded -> {
                notificationHelper.showFriendRequestNotification(
                    title = "新的朋友",
                    content = "你已添加了${event.nickname}，现在可以开始聊天了"
                )
            }
        }
    }

    /**
     * ✅ 处理新消息
     */
    private suspend fun handleNewMessage(message: ChatMessage) {
        if (message.isFromMe) return  // 自己发送的消息不通知

        val contentText = message.content.toPreviewText()

        // 查询联系人昵称
        val contact = contactRepository.getContactById(message.senderId)
        val senderName = contact?.displayName ?: "新消息"

        // 显示通知
        notificationHelper.showMessageNotification(
            sessionId = message.sessionId,
            title = senderName,
            content = contentText,
            notificationId = message.id.hashCode()
        )
    }

    // ==================== 前台服务 ====================

    /**
     * ✅ 只创建前台服务通道（其他通道由 NotificationHelper 创建）
     */
    private fun createForegroundServiceChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "P2P通信服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持设备发现和消息收发功能运行"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

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