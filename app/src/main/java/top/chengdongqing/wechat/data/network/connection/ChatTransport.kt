package top.chengdongqing.wechat.data.network.connection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import top.chengdongqing.wechat.data.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.data.network.protocol.Packet
import java.util.concurrent.ConcurrentHashMap

/**
 * 对 MessageReceiver 暴露的能力
 * 只关心：有新连接时给我一个可消费的 channel
 */
interface InboundTransport {
    val connectionEvents: Flow<ConnectionEvent>
}

/**
 * 对 MessageSender / TransferManager 暴露的能力
 * 只关心：我能发包
 */
interface OutboundTransport {
    /**
     * 需要用户介入才能建立连接时触发
     * 蓝牙/WiFi Direct 没有已知连接信息时使用
     */
    val connectionRequired: SharedFlow<ConnectionRequiredEvent>

    suspend fun send(userId: String, packet: Packet): Result<Unit>

    suspend fun sendAtomicTransfer(
        userId: String,
        block: suspend (EncryptingPacketWriter) -> Unit
    ): Result<Unit>

    fun isConnected(userId: String): Boolean

    suspend fun disconnect(userId: String)

    suspend fun disconnectAll()
}

/**
 * 完整传输能力 = 收 + 发
 */
interface ChatTransport : InboundTransport, OutboundTransport {
    val connections: ConcurrentHashMap<String, PeerConnection>
}