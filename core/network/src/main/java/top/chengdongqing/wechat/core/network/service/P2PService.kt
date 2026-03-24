package top.chengdongqing.wechat.core.network.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.common.di.IoScope
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.data.repository.ConnectionSettingsRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.network.R
import top.chengdongqing.wechat.core.network.ble.BluetoothStateMonitor
import top.chengdongqing.wechat.core.network.connection.ChatTransportManager
import top.chengdongqing.wechat.core.network.http.AvatarServer
import top.chengdongqing.wechat.core.network.model.NotificationChannelConfig
import top.chengdongqing.wechat.core.network.model.NotificationId
import top.chengdongqing.wechat.core.network.service.addfriend.BLEAddFriendHandler
import top.chengdongqing.wechat.core.network.service.call.CallServiceModule
import top.chengdongqing.wechat.core.network.service.chat.BtChatHandler
import top.chengdongqing.wechat.core.network.service.chat.WiFiDirectChatHandler
import top.chengdongqing.wechat.core.network.service.chat.WiFiLanChatHandler
import top.chengdongqing.wechat.core.network.service.notification.NotificationServiceModule
import javax.inject.Inject

@AndroidEntryPoint
class P2PService : Service() {

    @Inject
    lateinit var addFriendHandler: BLEAddFriendHandler

    @Inject
    lateinit var lanChatHandler: WiFiLanChatHandler

    @Inject
    lateinit var btChatHandler: BtChatHandler

    @Inject
    lateinit var directChatHandler: WiFiDirectChatHandler

    @Inject
    lateinit var callHandler: CallServiceModule

    @Inject
    lateinit var avatarServer: AvatarServer

    @Inject
    lateinit var notificationHandler: NotificationServiceModule

    @Inject
    lateinit var transportManager: ChatTransportManager

    @Inject
    lateinit var connectionSettingsRepository: ConnectionSettingsRepository

    @Inject
    lateinit var bluetoothStateMonitor: BluetoothStateMonitor

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    @IoScope
    lateinit var scope: CoroutineScope

    private var serviceJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        showForegroundNotification()

        bluetoothStateMonitor.start()

        serviceJob = scope.launch {
            observeMyProfile()
        }
    }

    /**
     * 监听个人信息变化，注册后自动启动相关服务，注销后自动停止
     */
    private suspend fun CoroutineScope.observeMyProfile() {
        profileRepository.observeProfile()
            .map { it?.id }
            .distinctUntilChanged()
            .collectLatest { myUserId ->
                if (myUserId != null) {
                    startModules()
                } else {
                    stopModules()
                }
            }
    }

    /**
     * 核心业务逻辑启动
     */
    private fun CoroutineScope.startModules() {
        // 启动常驻子服务
        callHandler.start()
        avatarServer.start()
        notificationHandler.start()

        // 监听连接模式，动态切换
        launch {
            connectionSettingsRepository.connectionMode
                .distinctUntilChanged()
                .collectLatest { mode ->
                    handleModeSwitch(mode)
                }
        }

        // 监听蓝牙状态，动态启停加好友模块
        launch {
            bluetoothStateMonitor.isAvailable.collectLatest { available ->
                if (available) {
                    addFriendHandler.start()
                } else {
                    addFriendHandler.stop()
                }
            }
        }
    }

    /**
     * 处理连接模式切换
     */
    private suspend fun handleModeSwitch(mode: ConnectionMode) {
        transportManager.setMode(mode)

        // 停止所有
        listOf(lanChatHandler, directChatHandler, btChatHandler).forEach {
            runCatching { it.stop() }
        }

        delay(500)

        // 启动对应模块
        when (mode) {
            ConnectionMode.WiFiLan -> lanChatHandler.start()
            ConnectionMode.WiFiDirect -> directChatHandler.start()
            ConnectionMode.Bluetooth -> btChatHandler.start()
        }
    }

    /**
     * 停止所有模块
     */
    private fun stopModules() {
        listOf(
            lanChatHandler, directChatHandler, btChatHandler,
            addFriendHandler, callHandler, avatarServer, notificationHandler
        ).forEach {
            runCatching { it.stop() }
        }
    }

    /**
     * 创建前台服务通知渠道
     */
    private fun createNotificationChannel() {
        NotificationChannel(
            NotificationChannelConfig.P2P.id,
            NotificationChannelConfig.P2P.title,
            NotificationChannelConfig.P2P.importance
        ).apply {
            description = NotificationChannelConfig.P2P.description
        }.also {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(it)
        }
    }

    /**
     * 显示前台服务通知
     */
    private fun showForegroundNotification() {
        // 创建点击通知后的Intent
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知
        val notification = NotificationCompat.Builder(this, NotificationChannelConfig.P2P.id)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.p2p_service_content))
            .setSmallIcon(R.drawable.img_logo)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        startForeground(NotificationId.P2P.id, notification)
    }

    override fun onDestroy() {
        stopModules()
        bluetoothStateMonitor.stop()
        serviceJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}