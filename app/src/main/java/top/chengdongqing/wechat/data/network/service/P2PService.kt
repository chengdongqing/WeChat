package top.chengdongqing.wechat.data.network.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.network.avatar.AvatarServer
import top.chengdongqing.wechat.data.network.connection.ChatTransportManager
import top.chengdongqing.wechat.data.network.connection.ConnectionMode
import top.chengdongqing.wechat.data.network.service.addfriend.BLEAddFriendModule
import top.chengdongqing.wechat.data.network.service.call.CallModule
import top.chengdongqing.wechat.data.network.service.chat.BluetoothChatModule
import top.chengdongqing.wechat.data.network.service.chat.WiFiDirectChatModule
import top.chengdongqing.wechat.data.network.service.chat.WiFiLanChatModule
import top.chengdongqing.wechat.data.network.service.notification.NotificationHelper
import top.chengdongqing.wechat.data.network.service.notification.NotificationModule
import top.chengdongqing.wechat.features.settings.domain.repository.ConnectionSettingsRepository
import javax.inject.Inject

@AndroidEntryPoint
class P2PService : Service() {
    companion object {
        private const val TAG = "P2PService"

        const val ACTION_START_SERVICE = "action_start_service" // 登录成功后调用
        const val ACTION_STOP_SERVICE = "action_stop_service"   // 退出登录调用
        const val ACTION_RETRY_BLE = "action_retry_ble"         // 权限授予后调用
    }

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
    lateinit var notificationHelper: NotificationHelper

    @Inject
    @IoScope
    lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()

        // 创建通知渠道
        notificationHelper.createNotificationChannels()
        // 显示前台服务通知
        showForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            when (intent?.action) {
                /**
                 * 服务启动入口
                 */
                ACTION_START_SERVICE -> startAll()
                /**
                 * 在获取蓝牙权限后重启蓝牙模块
                 */
                ACTION_RETRY_BLE -> bleAddFriendModule.start()
                /**
                 * 服务停止入口
                 */
                ACTION_STOP_SERVICE -> stopAll()
            }
        }

        return START_STICKY // 被系统杀死后自动重启，保持消息收发能力
    }

    /**
     * 启动所有服务
     */
    private fun startAll() {
        // 启动加好友模块
        bleAddFriendModule.start()
        // 启动通话模块（视频/语音通话）
        callModule.start()
        // 启动头像服务
        avatarServer.start()
        // 启动通知服务
        notificationModule.start()
        // 监听连接模式切换，启动对应聊天模块
        scope.launch { observeConnectionMode() }
    }

    /**
     * 停止所有服务
     */
    private fun stopAll() {
        bleAddFriendModule.stop()
        callModule.stop()
        avatarServer.stop()
        notificationModule.stop()
        wifiLanChatModule.stop()
        wifiDirectChatModule.stop()
        bluetoothChatModule.stop()
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
     * 显示前台服务通知
     */
    private fun showForegroundNotification() {
        val notification = NotificationCompat.Builder(this, NotificationHelper.P2P_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("服务运行中")
            .setSmallIcon(R.drawable.img_logo)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        startForeground(NotificationHelper.P2P_NOTIFICATION_ID, notification)
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