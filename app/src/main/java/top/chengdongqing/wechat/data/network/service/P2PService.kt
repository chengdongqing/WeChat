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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.network.avatar.AvatarServer
import top.chengdongqing.wechat.data.network.connection.ChatTransportManager
import top.chengdongqing.wechat.data.network.connection.ConnectionMode
import top.chengdongqing.wechat.data.network.model.NotificationChannelConfig
import top.chengdongqing.wechat.data.network.model.NotificationId
import top.chengdongqing.wechat.data.network.service.addfriend.BLEAddFriendModule
import top.chengdongqing.wechat.data.network.service.call.CallModule
import top.chengdongqing.wechat.data.network.service.chat.BluetoothChatModule
import top.chengdongqing.wechat.data.network.service.chat.WiFiDirectChatModule
import top.chengdongqing.wechat.data.network.service.chat.WiFiLanChatModule
import top.chengdongqing.wechat.data.network.service.notification.NotificationModule
import top.chengdongqing.wechat.features.settings.domain.repository.ConnectionSettingsRepository
import javax.inject.Inject

@AndroidEntryPoint
class P2PService : Service() {

    @Inject
    lateinit var bleAddFriendModule: BLEAddFriendModule

    @Inject
    lateinit var wifiLanChatModule: WiFiLanChatModule

    @Inject
    lateinit var bluetoothChatModule: BluetoothChatModule

    @Inject
    lateinit var wifiDirectChatModule: WiFiDirectChatModule

    @Inject
    lateinit var callModule: CallModule

    @Inject
    lateinit var avatarServer: AvatarServer

    @Inject
    lateinit var notificationModule: NotificationModule

    @Inject
    lateinit var transportManager: ChatTransportManager

    @Inject
    lateinit var connectionSettingsRepository: ConnectionSettingsRepository

    @Inject
    @IoScope
    lateinit var scope: CoroutineScope

    companion object {
        private const val TAG = "P2PService"

        const val ACTION_START_SERVICE = "action_start_service"
        const val ACTION_STOP_SERVICE = "action_stop_service"
        const val ACTION_RETRY_BLE = "action_retry_ble"
    }

    var hasStarted: Boolean = false

    override fun onCreate() {
        super.onCreate()

        // 创建前台服务通知渠道
        createNotificationChannel()
        // 显示前台服务通知
        showForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            when (intent?.action) {
                /**
                 * 服务启动入口
                 */
                ACTION_START_SERVICE -> {
                    if (!hasStarted) {
                        startAll()
                    }
                }
                /**
                 * 在获取蓝牙权限后重启蓝牙模块
                 */
                ACTION_RETRY_BLE -> {
                    bleAddFriendModule.stop()
                    bleAddFriendModule.start()
                }
                /**
                 * 服务停止入口
                 */
                ACTION_STOP_SERVICE -> stopAll()
            }
        }

        return START_STICKY // 被系统杀死后自动重启，保持消息收发能力
    }

    private var observerJob: Job? = null

    /**
     * 启动所有服务
     */
    private fun startAll() {
        // 监听连接模式切换，启动对应聊天模块
        observerJob = scope.launch { observeConnectionMode() }

        // 启动加好友模块
        bleAddFriendModule.start()
        // 启动通话模块（视频/语音通话）
        callModule.start()
        // 启动头像服务
        avatarServer.start()
        // 启动通知服务
        notificationModule.start()

        hasStarted = true
    }

    /**
     * 停止所有服务
     */
    private fun stopAll() {
        wifiLanChatModule.stop()
        wifiDirectChatModule.stop()
        bluetoothChatModule.stop()

        bleAddFriendModule.stop()
        callModule.stop()
        avatarServer.stop()
        notificationModule.stop()
        observerJob?.cancel()

        hasStarted = false
    }

    /**
     * 监听连接模式的变化
     */
    private suspend fun observeConnectionMode() {
        connectionSettingsRepository.connectionMode
            .collect { mode ->
                transportManager.setMode(mode)

                try {
                    // 停止所有聊天模块
                    wifiLanChatModule.stop()
                    wifiDirectChatModule.stop()
                    bluetoothChatModule.stop()

                    // 启动当前聊天模块
                    when (mode) {
                        ConnectionMode.WiFiLan -> wifiLanChatModule.start()
                        ConnectionMode.WiFiDirect -> wifiDirectChatModule.start()
                        ConnectionMode.Bluetooth -> bluetoothChatModule.start()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "连接模式切换异常", e)
                }
            }
    }

    /**
     * 创建前台服务通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                NotificationChannelConfig.P2P.id,
                NotificationChannelConfig.P2P.title,
                NotificationChannelConfig.P2P.importance
            ).apply {
                description = NotificationChannelConfig.P2P.description
            }.also {
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).apply {
                    createNotificationChannel(it)
                }
            }
        }
    }

    /**
     * 显示前台服务通知
     */
    private fun showForegroundNotification() {
        val notification = NotificationCompat.Builder(this, NotificationChannelConfig.P2P.id)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("服务运行中")
            .setSmallIcon(R.drawable.img_logo)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        startForeground(NotificationId.P2P.id, notification)
    }

    override fun onDestroy() {
        super.onDestroy()

        stopAll()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

fun Context.createNetworkServiceIntent(action: String): Intent {
    return Intent(this, P2PService::class.java).apply {
        this.action = action
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}