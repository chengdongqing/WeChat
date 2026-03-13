package top.chengdongqing.wechat.data.network.connection.wifi

import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import top.chengdongqing.wechat.data.network.connection.ChatTransport
import top.chengdongqing.wechat.data.network.connection.ConnectionEvent
import top.chengdongqing.wechat.data.network.connection.ConnectionRequiredEvent
import top.chengdongqing.wechat.data.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.data.network.protocol.Packet

class WiFiDirectChatTransport @Inject constructor() : ChatTransport {

    override val connectionEvents: Flow<ConnectionEvent> = emptyFlow()

    private val _connectionRequired =
        MutableSharedFlow<ConnectionRequiredEvent>(extraBufferCapacity = 8)
    override val connectionRequired: SharedFlow<ConnectionRequiredEvent> =
        _connectionRequired.asSharedFlow()

    override suspend fun send(userId: String, packet: Packet): Result<Unit> =
        Result.failure(NotImplementedError("WiFi Direct 暂未实现"))

    override suspend fun sendAtomicTransfer(
        userId: String,
        block: suspend (EncryptingPacketWriter) -> Unit
    ): Result<Unit> =
        Result.failure(NotImplementedError("WiFi Direct 暂未实现"))

    override fun isConnected(userId: String) = false

    override suspend fun disconnect(userId: String) {}

    override suspend fun disconnectAll() {}
}