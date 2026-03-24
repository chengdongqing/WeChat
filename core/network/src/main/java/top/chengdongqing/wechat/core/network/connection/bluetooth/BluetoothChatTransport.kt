package top.chengdongqing.wechat.core.network.connection.bluetooth

import jakarta.inject.Inject
import jakarta.inject.Singleton
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.core.network.connection.AbstractChatTransport
import top.chengdongqing.wechat.core.network.connection.ConnectionManager
import top.chengdongqing.wechat.core.network.connection.ConnectionRequiredEvent
import top.chengdongqing.wechat.core.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.core.network.model.Packet

/**
 * 蓝牙聊天传输层
 */
@Singleton
class BluetoothChatTransport @Inject constructor(
    private val socketClient: BluetoothSocketClient,
    override val connectionManager: ConnectionManager,
    private val connectionInfoDao: ConnectionInfoDao,
    private val profileRepository: ProfileRepository
) : AbstractChatTransport(connectionManager) {

    private val myUserId: String
        get() = profileRepository.requireUserId()

    override suspend fun send(userId: String, packet: Packet) = ensureConnected(userId, packet) {
        connectionManager.send(userId, packet)
    }

    override suspend fun sendAtomicTransfer(
        userId: String,
        block: suspend (EncryptingPacketWriter) -> Unit
    ) = ensureConnected(userId) {
        connectionManager.sendAtomicTransfer(userId, block)
    }

    override suspend fun tryAutoReconnect(userId: String): Boolean {
        val mac = connectionInfoDao.getById(userId)?.bluetoothAddress ?: return false

        return socketClient.connect(
            userId = userId,
            macAddress = mac,
            myUserId = myUserId
        ).isSuccess
    }

    override suspend fun <T> onConnectionUnavailable(userId: String, packet: Packet?) =
        requireConnectionFromUi(ConnectionRequiredEvent.Bluetooth)
}