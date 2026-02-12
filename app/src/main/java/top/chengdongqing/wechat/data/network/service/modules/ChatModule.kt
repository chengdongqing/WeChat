package top.chengdongqing.wechat.data.network.service.modules

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.entity.ConnectionInfoEntity
import top.chengdongqing.wechat.data.database.entity.ConnectionType
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.network.discovery.DiscoveredDevice
import top.chengdongqing.wechat.data.network.discovery.NSDDiscoveryManager
import top.chengdongqing.wechat.data.network.messaging.MessageReceiver
import top.chengdongqing.wechat.data.network.socket.SocketManager
import top.chengdongqing.wechat.data.network.socket.SocketServer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天模块 - 负责消息收发功能
 */
@Singleton
class ChatModule @Inject constructor(
    private val nsdManager: NSDDiscoveryManager,
    private val socketServer: SocketServer,
    private val socketManager: SocketManager,
    private val messageReceiver: MessageReceiver,
    private val connectionInfoDao: ConnectionInfoDao
) {

    private companion object {
        const val TAG = "ChatModule"
    }

    private val _newMessages = MutableSharedFlow<MessageEntity>()
    val newMessages: SharedFlow<MessageEntity> = _newMessages.asSharedFlow()

    /**
     * 启动聊天服务
     */
    fun start(userId: String, scope: CoroutineScope) {
        // 1. 启动 Socket 服务器
        socketServer.start()
        Log.d(TAG, "✅ Socket 服务器已启动")

        // 2. 注册 NSD 服务
        scope.launch {
            nsdManager.registerService(userId).collect { state ->
                Log.d(TAG, "NSD 状态: $state")
            }
        }

        // 3. 发现其他设备
        scope.launch {
            nsdManager.discoverServices().collect { device ->
                handleDiscoveredDevice(device)
            }
        }

        // 4. 启动消息接收器
        messageReceiver.start()
        Log.d(TAG, "✅ 消息接收器已启动")

        // 5. 监听连接事件
        scope.launch {
            socketManager.connectionEvents.collect { event ->
                Log.d(TAG, "连接事件: $event")
            }
        }

        // 6. 转发新消息
        scope.launch {
            messageReceiver.newMessages.collect { message ->
                _newMessages.emit(message)
            }
        }

        Log.d(TAG, "✅ 聊天模块已启动")
    }

    /**
     * 停止聊天服务
     */
    fun stop() {
        socketServer.stop()
        socketManager.closeAll()
        Log.d(TAG, "聊天模块已停止")
    }

    /**
     * 处理发现的设备
     */
    private suspend fun handleDiscoveredDevice(device: DiscoveredDevice) {
        Log.d(TAG, "发现设备: ${device.userId} @ ${device.host}:${device.port}")

        // 保存连接信息
        val connectionInfo = ConnectionInfoEntity(
            userId = device.userId,
            connectionType = ConnectionType.WiFiLan,
            ipAddress = device.host,
            port = device.port,
            serviceName = device.serviceName,
            isOnline = true,
            lastSeen = System.currentTimeMillis(),
            priority = 0,  // WiFi LAN 优先级最高
            updatedAt = System.currentTimeMillis()
        )

        connectionInfoDao.insert(connectionInfo)

        // 自动连接
        socketManager.connect(device.userId, device.host, device.port)
            .onSuccess { connection ->
                Log.d(TAG, "✅ 已连接: ${device.userId}")
                // 开始监听消息
                messageReceiver.startListening(connection)
            }
            .onFailure { error ->
                Log.e(TAG, "连接失败: ${device.userId}", error)
            }
    }
}