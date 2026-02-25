package top.chengdongqing.wechat.data.network.service.modules

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.entity.ConnectionInfoEntity
import top.chengdongqing.wechat.data.database.entity.ConnectionType
import top.chengdongqing.wechat.data.network.connection.ConnectionManager
import top.chengdongqing.wechat.data.network.discovery.DiscoveredDevice
import top.chengdongqing.wechat.data.network.discovery.DiscoveryEvent
import top.chengdongqing.wechat.data.network.discovery.NSDDiscovery
import top.chengdongqing.wechat.data.network.discovery.ServiceRegistrationState
import top.chengdongqing.wechat.data.network.messaging.MessageReceiver
import top.chengdongqing.wechat.data.network.service.NetworkService
import top.chengdongqing.wechat.data.network.socket.ConnectionEvent
import top.chengdongqing.wechat.data.network.socket.SocketClient
import top.chengdongqing.wechat.data.network.socket.SocketServer
import top.chengdongqing.wechat.data.network.transfer.WifiLockManager
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天模块
 *
 * LAN 消息收发的完整生命周期管理，启动顺序：
 * 1. [SocketServer] 启动并绑定随机端口
 * 2. NSD 注册：将本机服务广播到局域网，携带端口和 userId
 * 3. NSD 发现：扫描局域网内其他用户，发现后主动建立 TCP 连接
 * 4. [MessageReceiver] 启动，消费各连接的收包 Channel
 * 5. 订阅 [SocketClient] 连接事件，连接建立时自动启动包监听
 */
@Singleton
class ChatModule @Inject constructor(
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

    // ==================== 启停 ====================

    suspend fun start(userId: String, scope: CoroutineScope) {
        wifiLockManager.acquireKeepAlive()

        val port = socketServer.start()
        Log.d(TAG, "Socket 服务端已启动，端口: $port")

        observeConnectionEvents(scope)
        startNsdRegistration(userId, port, scope)
        startNsdDiscovery(userId, scope)

        messageReceiver.start()
        Log.d(TAG, "聊天模块已启动")
    }

    fun stop() {
        wifiLockManager.releaseKeepAlive()
        socketServer.stop()
        connectionManager.closeAll()
        Log.d(TAG, "聊天模块已停止")
    }

    // ==================== NSD ====================

    /**
     * 启动 NSD 注册
     *
     * 将本机服务广播到局域网，其他设备发现后通过 TXT 属性的 userId 识别身份。
     * 注：[ServiceRegistrationState.Registered] 里的 port 由系统回调返回，部分设备会返回 0，
     * 实际注册端口以传入的 [port] 为准，详见 [NSDDiscovery.registerService]。
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
     * DeviceFound：持久化连接信息并主动建立 TCP 连接
     * DeviceLost：标记离线并断开连接
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

    // ==================== 设备发现 ====================

    /**
     * 处理新发现的设备
     *
     * 每次发现都更新数据库中的连接信息（对方可能重启过，端口已变）。
     * 已有出站连接则跳过建连（幂等），否则主动发起 TCP 连接。
     * 连接建立后由 [observeConnectionEvents] 统一启动包监听。
     */
    private suspend fun handleDeviceFound(device: DiscoveredDevice, myUserId: String) {
        // 是否是连接状态
        if (connectionManager.isConnected(device.userId)) {
            Log.d(TAG, "发现设备 - 设备已连接，跳过: ${device.userId}")
            return
        }
        // 是否是好友
        if (!contactRepository.exists(device.userId)) {
            Log.d(TAG, "发现设备 - 对方不是好友，跳过: ${device.userId}")
            return
        }

        Log.d(TAG, "发现设备: ${device.userId} @ ${device.host}:${device.port}")
        socketClient.connect(
            userId = device.userId,
            host = device.host,
            port = device.port,
            myUserId = myUserId
        ).onSuccess {
            Log.d(TAG, "Socket 已连接: ${device.userId}")
        }.onFailure {
            Log.e(TAG, "连接失败: ${device.userId} - ${it.message}")
        }

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
    }

    /**
     * 处理设备离线
     *
     * serviceName 格式为 `WeChat_{userId}_{timestamp}`，
     * 取第二段作为 userId（兼容加时间戳后的格式）。
     */
    private suspend fun handleDeviceLost(serviceName: String) {
        val userId = serviceName.removePrefix("WeChat_").substringBefore("_")
        Log.d(TAG, "设备离线: $userId")
        connectionInfoDao.markOffline(userId)
        socketClient.disconnect(userId)
    }

    // ==================== 连接监听 ====================

    /**
     * 订阅 [SocketClient] 的连接状态事件
     *
     * Connected：将连接实例交给 [MessageReceiver] 开始消费收包 Channel
     * Disconnected：打日志，资源清理由 [SocketClient] 内部负责
     */
    private fun observeConnectionEvents(scope: CoroutineScope) {
        scope.launch {
            socketClient.connectionEvents.collect { event ->
                when (event) {
                    is ConnectionEvent.Connected -> {
                        connectionManager.getConnection(event.userId)?.let { connection ->
                            messageReceiver.startListening(connection)
                            Log.d(TAG, "开始监听连接: ${event.userId}")
                        }
                    }

                    is ConnectionEvent.Disconnected ->
                        Log.d(TAG, "连接断开: ${event.userId} - ${event.reason}")
                }
            }
        }
    }
}