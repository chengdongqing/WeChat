package top.chengdongqing.wechat.data.network.connection.wifi

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.model.SendError
import top.chengdongqing.wechat.data.network.connection.ChatTransport
import top.chengdongqing.wechat.data.network.connection.ConnectionEvent
import top.chengdongqing.wechat.data.network.connection.ConnectionException
import top.chengdongqing.wechat.data.network.connection.ConnectionRequiredEvent
import top.chengdongqing.wechat.data.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository

/**
 * Wi-Fi LAN 传输层
 */
@Singleton
class WiFiLanChatTransport @Inject constructor(
    private val socketClient: SocketClient,
    private val connectionManager: ConnectionManager,
    private val connectionInfoDao: ConnectionInfoDao,
    private val profileRepository: ProfileRepository
) : ChatTransport {

    private val myUserId get() = profileRepository.getProfile()?.id!!

    override val connectionEvents: Flow<ConnectionEvent> = connectionManager.connectionEvents

    private val _connectionRequired =
        MutableSharedFlow<ConnectionRequiredEvent>(extraBufferCapacity = 8)
    override val connectionRequired: SharedFlow<ConnectionRequiredEvent> =
        _connectionRequired.asSharedFlow()

    override val connections = connectionManager.connections

    override suspend fun send(userId: String, packet: Packet): Result<Unit> {
        return ensureConnected(userId) {
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
        action: suspend () -> Result<T>
    ): Result<T> {
        // 已连接直接执行
        if (connectionManager.isConnected(userId)) {
            return action()
        }

        // 有历史地址，尝试自动静默连接
        val info = connectionInfoDao.getById(userId)
        if (info?.lanIpAddress != null && info.lanPort != null) {
            val connectResult = socketClient.connect(
                userId = userId,
                host = info.lanIpAddress,
                port = info.lanPort,
                myUserId = myUserId
            )

            if (connectResult.isSuccess) {
                return action()
            }
        }

        return Result.failure(
            ConnectionException(
                message = "未找到连接信息: $userId",
                failReason = SendError.ConnectionFailed
            )
        )
    }

    override fun isConnected(userId: String) =
        connectionManager.isConnected(userId)

    override suspend fun disconnect(userId: String) =
        connectionManager.disconnect(userId)

    override suspend fun disconnectAll() =
        connectionManager.closeAll()
}