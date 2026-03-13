package top.chengdongqing.wechat.data.network.connection.wifi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import top.chengdongqing.wechat.data.network.connection.ChatTransport
import top.chengdongqing.wechat.data.network.connection.ConnectionEvent
import top.chengdongqing.wechat.data.network.connection.ConnectionRequiredEvent
import top.chengdongqing.wechat.data.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.data.network.protocol.Packet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WiFiDirectChatTransport @Inject constructor(
    private val connectionManager: ConnectionManager
) : ChatTransport {

    private val _connectionRequired =
        MutableSharedFlow<ConnectionRequiredEvent>(extraBufferCapacity = 8)
    override val connectionRequired: SharedFlow<ConnectionRequiredEvent> =
        _connectionRequired.asSharedFlow()

    override val connectionEvents: Flow<ConnectionEvent> =
        connectionManager.connectionEvents

    override suspend fun send(userId: String, packet: Packet): Result<Unit> {
        if (connectionManager.isConnected(userId)) {
            return connectionManager.send(userId, packet)
        }

        // 没有连接，通知 UI 弹出设备选择弹窗
        _connectionRequired.emit(ConnectionRequiredEvent.WiFiDirect(userId, packet))
        return Result.failure(Exception("需要选择 WiFi Direct 设备"))
    }

    override suspend fun sendAtomicTransfer(
        userId: String,
        block: suspend (EncryptingPacketWriter) -> Unit
    ) = connectionManager.sendAtomicTransfer(userId, block)

    override fun isConnected(userId: String) = connectionManager.isConnected(userId)

    override suspend fun disconnect(userId: String) = connectionManager.disconnect(userId)

    override suspend fun disconnectAll() = connectionManager.closeAll()
}