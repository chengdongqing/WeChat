package top.chengdongqing.wechat.data.network.service.modules

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.entity.ConnectionInfoEntity
import top.chengdongqing.wechat.data.network.connection.ConnectionMode
import top.chengdongqing.wechat.data.network.connection.wifi.ConnectionManager
import top.chengdongqing.wechat.data.network.connection.wifi.SocketClient
import top.chengdongqing.wechat.data.network.connection.wifi.SocketServer
import top.chengdongqing.wechat.data.network.discovery.DiscoveredDevice
import top.chengdongqing.wechat.data.network.discovery.DiscoveryEvent
import top.chengdongqing.wechat.data.network.discovery.NSDDiscovery
import top.chengdongqing.wechat.data.network.discovery.ServiceRegistrationState
import top.chengdongqing.wechat.data.network.messaging.MessageReceiver
import top.chengdongqing.wechat.data.network.service.NetworkService
import top.chengdongqing.wechat.data.network.transfer.WifiLockManager
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天模块
 */
@Singleton
class WiFiLanChatModule @Inject constructor(
    private val nsdDiscovery: NSDDiscovery,
    private val socketServer: SocketServer,
    private val socketClient: SocketClient,
    private val connectionManager: ConnectionManager,
    private val wifiLockManager: WifiLockManager,
    private val messageReceiver: MessageReceiver,
    private val connectionInfoDao: ConnectionInfoDao,
    private val contactRepository: ContactRepository
) {
    private companion object {
        const val TAG = "ChatModule"
    }

    /** 透传 [MessageReceiver] 的新消息流，供 [NetworkService] 订阅通知 */
    val incomingMessageFlow: SharedFlow<ChatMessage>
        get() = messageReceiver.incomingMessageFlow

    /**
     * 启动聊天服务模块
     */
    suspend fun start(userId: String, scope: CoroutineScope) {
        // 申请Wi-Fi锁，后台通信保活
        wifiLockManager.acquireKeepAlive()
        // 启动TCP服务
        val port = socketServer.start()
        // 注册NSD服务
        startNsdRegistration(userId, port, scope)
        // 开始搜索NSD设备
        startNsdDiscovery(userId, scope)
        // 启动消息接收服务
        messageReceiver.start()

        Log.d(TAG, "Wi-Fi Lan 聊天模块已启动")
    }

    fun stop() {
        // 停止TCP服务
        socketServer.stop()
        // 关闭所有连接
        connectionManager.closeAll()
        // 释放Wi-Fi锁
        wifiLockManager.releaseKeepAlive()

        Log.d(TAG, "Wi-Fi Lan 聊天模块已停止")
    }

    /**
     * 启动 NSD 注册
     *
     * 将本机服务广播到局域网，其他设备发现后通过 TXT 属性的 userId 识别身份。
     */
    private fun startNsdRegistration(userId: String, port: Int, scope: CoroutineScope) {
        scope.launch {
            nsdDiscovery.registerService(userId, port).collect { state ->
                when (state) {
                    is ServiceRegistrationState.Registered ->
                        Log.d(TAG, "NSD 注册成功，端口: $port")

                    is ServiceRegistrationState.Failed ->
                        Log.e(TAG, "NSD 注册失败: errorCode=${state.errorCode}")

                    is ServiceRegistrationState.Unregistered ->
                        Log.d(TAG, "NSD 已注销")
                }
            }
        }
    }

    /**
     * 启动 NSD 服务发现
     *
     * 持续监听局域网内设备上下线事件。
     */
    private fun startNsdDiscovery(userId: String, scope: CoroutineScope) {
        scope.launch {
            nsdDiscovery.discoverServices(userId).collect { event ->
                when (event) {
                    is DiscoveryEvent.DeviceFound -> handleDeviceFound(event.device, userId)
                    is DiscoveryEvent.DeviceLost -> handleDeviceLost(event.serviceName)
                }
            }
        }
    }

    /**
     * 处理新发现的设备
     */
    private suspend fun handleDeviceFound(device: DiscoveredDevice, myUserId: String) {
        // 保存连接信息
        connectionInfoDao.insertOrUpdate(
            ConnectionInfoEntity(
                userId = device.userId,
                connectionMode = ConnectionMode.WiFiLan,
                ipAddress = device.host,
                port = device.port,
                serviceName = device.serviceName,
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                priority = 0
            )
        )

        // 已连接跳过
        if (connectionManager.isConnected(device.userId)) {
            return
        }
        // 不是好友跳过
        if (!contactRepository.exists(device.userId)) {
            return
        }

        // 尝试连接
        socketClient.connect(
            userId = device.userId,
            host = device.host,
            port = device.port,
            myUserId = myUserId
        ).onSuccess {
            Log.d(TAG, "Socket 已连接: ${device.userId}")
        }
    }

    /**
     * 处理设备离线
     *
     * serviceName 格式为 `WeChat_{userId}_{timestamp}`，
     * 取第二段作为 userId（兼容加时间戳后的格式）。
     */
    private suspend fun handleDeviceLost(serviceName: String) {
        val userId = serviceName.removePrefix("WeChat_").substringBeforeLast("_")
        connectionManager.disconnect(userId)
    }
}