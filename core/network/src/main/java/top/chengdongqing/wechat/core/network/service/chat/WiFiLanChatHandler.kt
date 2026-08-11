package top.chengdongqing.wechat.core.network.service.chat

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.repository.ContactRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.core.database.entity.ConnectionInfoEntity
import top.chengdongqing.wechat.core.network.connection.ConnectionManager
import top.chengdongqing.wechat.core.network.connection.wifi.DiscoveredDevice
import top.chengdongqing.wechat.core.network.connection.wifi.DiscoveryEvent
import top.chengdongqing.wechat.core.network.connection.wifi.NsdDiscovery
import top.chengdongqing.wechat.core.network.connection.wifi.ServiceRegistrationState
import top.chengdongqing.wechat.core.network.connection.wifi.TcpSocketClient
import top.chengdongqing.wechat.core.network.connection.wifi.TcpSocketServer
import top.chengdongqing.wechat.core.network.messaging.MessageReceiver
import top.chengdongqing.wechat.core.network.service.ServiceModule
import top.chengdongqing.wechat.core.network.transfer.WiFiLockManager
import top.chengdongqing.wechat.core.runtime.IoScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wi-Fi局域网聊天模块
 */
@Singleton
class WiFiLanChatHandler @Inject constructor(
    private val nsdDiscovery: NsdDiscovery,
    private val socketServer: TcpSocketServer,
    private val socketClient: TcpSocketClient,
    private val connectionManager: ConnectionManager,
    private val wifiLockManager: WiFiLockManager,
    private val messageReceiver: MessageReceiver,
    private val connectionInfoDao: ConnectionInfoDao,
    private val contactRepository: ContactRepository,
    private val profileRepository: ProfileRepository,
    @param:IoScope private val scope: CoroutineScope
) : ServiceModule {
    private companion object {
        const val TAG = "WiFiLanChatHandler"
    }

    private val myUserId: String
        get() = profileRepository.requireUserId()

    private var observeJob: Job? = null

    override fun start() {
        runCatching {
            // 申请Wi-Fi锁，后台通信保活
            wifiLockManager.acquireKeepAlive()

            observeJob = scope.launch {
                // 启动TCP服务
                val port = socketServer.start()
                // 注册NSD服务
                launch { startNsdRegistration(port) }
                // 开始搜索NSD设备
                launch { startNsdDiscovery() }
                // 启动消息接收服务
                messageReceiver.start()
            }
        }.onSuccess {
            Log.d(TAG, "Wi-Fi Lan 聊天模块已启动")
        }.onFailure {
            Log.e(TAG, "Wi-Fi Lan 聊天模块启动失败", it)
        }
    }

    override fun stop() {
        runCatching {
            observeJob?.cancel()
            // 关闭所有连接
            connectionManager.closeAll()
            // 停止TCP服务
            socketServer.stop()
            // 释放Wi-Fi锁
            wifiLockManager.releaseKeepAlive()
        }.onSuccess {
            Log.d(TAG, "Wi-Fi Lan 聊天模块已停止")
        }
    }

    /**
     * 启动 NSD 注册
     *
     * 将本机服务广播到局域网，其他设备发现后通过 TXT 属性的 userId 识别身份。
     */
    private suspend fun startNsdRegistration(port: Int) {
        nsdDiscovery.registerService(myUserId, port).collect { state ->
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

    /**
     * 启动 NSD 服务发现
     *
     * 持续监听局域网内设备上下线事件。
     */
    private suspend fun startNsdDiscovery() {
        nsdDiscovery.discoverServices(myUserId).collect { event ->
            when (event) {
                is DiscoveryEvent.DeviceFound -> handleDeviceFound(event.device, myUserId)
                is DiscoveryEvent.DeviceLost -> handleDeviceLost(event.serviceName)
            }
        }
    }

    /**
     * 处理新发现的设备
     */
    private suspend fun handleDeviceFound(device: DiscoveredDevice, myUserId: String) {
        // 保存连接信息
        connectionInfoDao.upsert(
            ConnectionInfoEntity(
                userId = device.userId,
                lanIpAddress = device.host,
                lanPort = device.port,
                lanServiceName = device.serviceName,
                isOnline = true,
                lastSeen = System.currentTimeMillis()
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