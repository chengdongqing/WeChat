package top.chengdongqing.wechat.core.network.connection.wifi

import top.chengdongqing.wechat.core.network.connection.AbstractChatTransport
import top.chengdongqing.wechat.core.network.connection.ConnectionManager
import top.chengdongqing.wechat.core.network.connection.ConnectionRequiredEvent
import top.chengdongqing.wechat.core.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.core.network.model.Packet
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wi-Fi Direct 传输层
 */
@Singleton
class WiFiDirectChatTransport @Inject constructor(
    override val connectionManager: ConnectionManager
) : AbstractChatTransport(connectionManager) {

    override suspend fun send(userId: String, packet: Packet) = ensureConnected(userId, packet) {
        connectionManager.send(userId, packet)
    }

    override suspend fun sendAtomicTransfer(
        userId: String,
        block: suspend (EncryptingPacketWriter) -> Unit
    ) = ensureConnected(userId) {
        connectionManager.sendAtomicTransfer(userId, block)
    }

    override suspend fun <T> onConnectionUnavailable(userId: String, packet: Packet?) =
        requireConnectionFromUi(ConnectionRequiredEvent.WiFiDirect)
}