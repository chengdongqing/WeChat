package top.chengdongqing.wechat.data.network.connection.wifi

import jakarta.inject.Inject
import jakarta.inject.Singleton
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.model.SendError
import top.chengdongqing.wechat.data.network.connection.BaseChatTransport
import top.chengdongqing.wechat.data.network.connection.ConnectionException
import top.chengdongqing.wechat.data.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.data.network.model.Packet
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository

/**
 * Wi-Fi LAN 传输层
 */
@Singleton
class WiFiLanChatTransport @Inject constructor(
    private val socketClient: TcpSocketClient,
    override val connectionManager: TcpConnectionManager,
    private val connectionInfoDao: ConnectionInfoDao,
    private val profileRepository: ProfileRepository
) : BaseChatTransport(connectionManager) {

    private val myUserId: String by lazy { profileRepository.requireUserId() }

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
            myUserId = myUserId,
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