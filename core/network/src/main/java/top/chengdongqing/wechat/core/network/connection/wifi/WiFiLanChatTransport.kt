package top.chengdongqing.wechat.core.network.connection.wifi

import jakarta.inject.Inject
import jakarta.inject.Singleton
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.core.model.SendError
import top.chengdongqing.wechat.core.network.connection.AbstractChatTransport
import top.chengdongqing.wechat.core.network.connection.ConnectionException
import top.chengdongqing.wechat.core.network.connection.ConnectionManager
import top.chengdongqing.wechat.core.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.core.network.model.Packet

/**
 * Wi-Fi LAN 传输层
 */
@Singleton
class WiFiLanChatTransport @Inject constructor(
    private val socketClient: TcpSocketClient,
    override val connectionManager: ConnectionManager,
    private val connectionInfoDao: ConnectionInfoDao,
    private val profileRepository: ProfileRepository
) : AbstractChatTransport(connectionManager) {

    private val myUserId: String
        get() = profileRepository.requireUserId()

    override suspend fun send(userId: String, packet: Packet) = ensureConnected(userId) {
        connectionManager.send(userId, packet)
    }

    override suspend fun sendAtomicTransfer(
        userId: String,
        block: suspend (EncryptingPacketWriter) -> Unit
    ) = ensureConnected(userId) {
        connectionManager.sendAtomicTransfer(userId, block)
    }

    override suspend fun tryAutoReconnect(userId: String): Boolean {
        val info = connectionInfoDao.getById(userId)
            ?.takeIf { it.lanIpAddress != null && it.lanPort != null }
            ?: return false

        return socketClient.connect(
            userId = userId,
            host = info.lanIpAddress!!,
            port = info.lanPort!!,
            myUserId = myUserId
        ).isSuccess
    }

    override suspend fun <T> onConnectionUnavailable(userId: String, packet: Packet?) =
        Result.failure<T>(
            ConnectionException(
                "未找到连接信息: $userId",
                SendError.ConnectionFailed
            )
        )
}