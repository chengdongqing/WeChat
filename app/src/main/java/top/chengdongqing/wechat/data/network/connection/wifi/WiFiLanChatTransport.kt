package top.chengdongqing.wechat.data.network.connection.wifi

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import top.chengdongqing.wechat.data.network.connection.ChatTransport
import top.chengdongqing.wechat.data.network.connection.ConnectionEvent
import top.chengdongqing.wechat.data.network.connection.ConnectionRequiredEvent
import top.chengdongqing.wechat.data.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.data.network.protocol.Packet

/**
 * Wi-Fi LAN 传输层
 */
@Singleton
class WiFiLanChatTransport @Inject constructor(
    private val connectionManager: ConnectionManager
) : ChatTransport {

    override val connectionEvents: Flow<ConnectionEvent> =
        connectionManager.connectionEvents

    private val _connectionRequired =
        MutableSharedFlow<ConnectionRequiredEvent>(extraBufferCapacity = 8)
    override val connectionRequired: SharedFlow<ConnectionRequiredEvent> =
        _connectionRequired.asSharedFlow()

    override suspend fun send(userId: String, packet: Packet) =
        connectionManager.send(userId, packet)

    override suspend fun sendAtomicTransfer(
        userId: String,
        block: suspend (EncryptingPacketWriter) -> Unit
    ) = connectionManager.sendAtomicTransfer(userId, block)

    override fun isConnected(userId: String) =
        connectionManager.isConnected(userId)

    override suspend fun disconnect(userId: String) =
        connectionManager.disconnect(userId)

    override suspend fun disconnectAll() =
        connectionManager.closeAll()
}