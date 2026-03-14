package top.chengdongqing.wechat.data.network.connection.wifi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.network.connection.ChatTransport
import top.chengdongqing.wechat.data.network.connection.ConnectionEvent
import top.chengdongqing.wechat.data.network.connection.ConnectionRequiredEvent
import top.chengdongqing.wechat.data.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WiFiDirectChatTransport @Inject constructor(
    private val socketClient: SocketClient,
    private val connectionManager: ConnectionManager,
    private val connectionInfoDao: ConnectionInfoDao,
    private val profileRepository: ProfileRepository
) : ChatTransport {

    private val myUserId get() = profileRepository.getProfile()?.id!!

    private val _connectionRequired =
        MutableSharedFlow<ConnectionRequiredEvent>(extraBufferCapacity = 8)
    override val connectionRequired: SharedFlow<ConnectionRequiredEvent> =
        _connectionRequired.asSharedFlow()

    override val connectionEvents: Flow<ConnectionEvent> =
        connectionManager.connectionEvents

    override val connections = connectionManager.connections

    override suspend fun send(userId: String, packet: Packet): Result<Unit> {
        return ensureConnected(userId, packet) {
            connectionManager.send(userId, packet)
        }
    }

    override suspend fun sendAtomicTransfer(
        userId: String,
        block: suspend (EncryptingPacketWriter) -> Unit
    ): Result<Unit> {
        return ensureConnected(userId) {
            connectionManager.sendAtomicTransfer(userId, block)
        }
    }

    /**
     * 连接守卫：确保已连接，否则尝试静默连接或发起 UI 请求
     */
    private suspend fun <T> ensureConnected(
        userId: String,
        packet: Packet? = null,
        action: suspend () -> Result<T>
    ): Result<T> {
        // 已连接直接执行
        if (connectionManager.isConnected(userId)) {
            return action()
        }

        // 有历史地址，尝试自动静默连接
        val info = connectionInfoDao.getById(userId)
        if (info?.p2pIpAddress != null && info.p2pPort != null) {
            val connectResult = socketClient.connect(
                userId = userId,
                host = info.p2pIpAddress,
                port = info.p2pPort,
                myUserId = myUserId
            )

            if (connectResult.isSuccess) {
                return action()
            }
        }

        // 完全没连接，抛出事件让 UI 处理
        _connectionRequired.emit(ConnectionRequiredEvent.WiFiDirect(userId, packet))
        return Result.failure(Exception("需要选择 WiFi Direct 设备"))
    }

    override fun isConnected(userId: String) = connectionManager.isConnected(userId)

    override suspend fun disconnect(userId: String) = connectionManager.disconnect(userId)

    override suspend fun disconnectAll() = connectionManager.closeAll()
}