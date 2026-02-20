package top.chengdongqing.wechat.data.network.service.modules

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.entity.ConnectionInfoEntity
import top.chengdongqing.wechat.data.database.entity.ConnectionType
import top.chengdongqing.wechat.data.network.discovery.DiscoveredDevice
import top.chengdongqing.wechat.data.network.discovery.DiscoveryEvent
import top.chengdongqing.wechat.data.network.discovery.NSDDiscovery
import top.chengdongqing.wechat.data.network.discovery.ServiceRegistrationState
import top.chengdongqing.wechat.data.network.messaging.MessageReceiver
import top.chengdongqing.wechat.data.network.socket.ConnectionEvent
import top.chengdongqing.wechat.data.network.socket.SocketClient
import top.chengdongqing.wechat.data.network.socket.SocketServer
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天模块 - 负责消息收发功能
 */
@Singleton
class ChatModule @Inject constructor(
    private val nsdManager: NSDDiscovery,
    private val socketServer: SocketServer,
    private val socketClient: SocketClient,
    private val messageReceiver: MessageReceiver,
    private val connectionInfoDao: ConnectionInfoDao
) {

    private companion object {
        const val TAG = "ChatModule"
    }

    // 透传 Flow
    val incomingMessageFlow: SharedFlow<ChatMessage>
        get() = messageReceiver.incomingMessageFlow

    // ==================== 启停 ====================

    suspend fun start(userId: String, scope: CoroutineScope) {
        // 1. 启动 Socket 服务器
        val port = socketServer.start()
        Log.d(TAG, "✅ Socket 服务器已启动")

        // 2. 注册 NSD 服务
        scope.launch {
            nsdManager.registerService(userId, port).collect { state ->
                if (state is ServiceRegistrationState.Registered) {
                    Log.d(TAG, "✅ NSD 已注册，端口: ${state.port}")
                } else {
                    Log.d(TAG, "NSD 状态: $state")
                }
            }
        }

        // 3. 发现其他设备
        scope.launch {
            nsdManager.discoverServices(userId).collect { event ->
                when (event) {
                    is DiscoveryEvent.DeviceFound ->
                        handleDiscoveredDevice(event.device, userId)

                    is DiscoveryEvent.DeviceLost ->
                        handleDeviceLost(event.serviceName)
                }
            }
        }

        // 4. 启动消息接收器
        messageReceiver.start()
        Log.d(TAG, "✅ 消息接收器已启动")

        // 5. 监听连接事件（重连时自动开始监听）
        observeNewConnections(scope)

        Log.d(TAG, "✅ 聊天模块已启动")
    }

    fun stop() {
        socketServer.stop()
        socketClient.closeAll()
        Log.d(TAG, "聊天模块已停止")
    }

    // ==================== 私有方法 ====================

    private suspend fun handleDiscoveredDevice(device: DiscoveredDevice, myUserId: String) {
        // 幂等性检查：如果已经在线或正在连接，跳过
        if (socketClient.isConnected(device.userId)) {
            Log.d(TAG, "设备 ${device.userId} 已连接，跳过重复连接")
            return
        }

        Log.d(TAG, "发现设备: ${device.userId} @ ${device.host}:${device.port}")

        // 保存连接信息
        connectionInfoDao.insert(
            ConnectionInfoEntity(
                userId = device.userId,
                connectionType = ConnectionType.WiFiLan,
                ipAddress = device.host,
                port = device.port,
                serviceName = device.serviceName,
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                priority = 0,
                updatedAt = System.currentTimeMillis()
            )
        )

        // 建立 Socket 连接
        socketClient.connect(
            userId = device.userId,
            host = device.host,
            port = device.port,
            myUserId = myUserId
        ).onSuccess {
            Log.d(TAG, "✅ Socket 已连接: ${device.userId}")
            // 不需要手动 startListening，observeNewConnections 统一处理
        }.onFailure { error ->
            Log.e(TAG, "❌ 连接失败: ${device.userId} - ${error.message}")
        }
    }

    private suspend fun handleDeviceLost(serviceName: String) {
        // serviceName 格式：WeChat_{userId}
        val userId = serviceName.removePrefix("WeChat_")
        Log.d(TAG, "设备离线: $userId")

        connectionInfoDao.markOffline(userId)
        socketClient.disconnect(userId)
    }

    private fun observeNewConnections(scope: CoroutineScope) {
        scope.launch {
            socketClient.connectionEvents.collect { event ->
                when (event) {
                    is ConnectionEvent.Connected -> {
                        // 统一在这里开始监听，无论是主动连接还是重连
                        val connection = socketClient.getConnection(event.userId)
                        if (connection != null) {
                            messageReceiver.startListening(connection)
                            Log.d(TAG, "✅ 开始监听: ${event.userId}")
                        }
                    }

                    is ConnectionEvent.Disconnected -> {
                        Log.d(TAG, "连接断开: ${event.userId} - ${event.reason}")
                    }
                }
            }
        }
    }
}