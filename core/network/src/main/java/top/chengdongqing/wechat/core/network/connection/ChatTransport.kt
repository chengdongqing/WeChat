package top.chengdongqing.wechat.core.network.connection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import top.chengdongqing.wechat.core.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.core.network.model.Packet

/**
 * 传输层抽象。
 *
 * 屏蔽底层连接方式（TCP / Bluetooth / WiFi Direct），
 * 上层只需关心连接事件和收发数据包。
 */
interface ChatTransport {

    /**
     * 新连接建立或连接断开时发出，消费方负责初始化/清理会话
     */
    val connectionEvents: Flow<ConnectionEvent>

    /**
     * 需要用户介入才能建立连接时发出。
     * 适用于无法自动重连的传输方式（如蓝牙、WiFi Direct），
     * UI 层收到后应引导用户手动操作。
     */
    val connectionRequired: SharedFlow<ConnectionRequiredEvent>

    /**
     * 当前是否与 [userId] 保持着活跃连接
     */
    fun isConnected(userId: String): Boolean

    /**
     * 向 [userId] 发送一个数据包，连接不存在或发送失败时返回 [Result.failure]
     */
    suspend fun send(userId: String, packet: Packet): Result<Unit>

    /**
     * 向 [userId] 发送一次原子传输，适用于需要独占写通道的场景（如文件传输）。
     *
     * [block] 在独占的 [EncryptingPacketWriter] 上执行，
     * block 结束前其他写操作会被阻塞。
     */
    suspend fun sendAtomicTransfer(
        userId: String,
        block: suspend (EncryptingPacketWriter) -> Unit,
    ): Result<Unit>

    /**
     * 主动断开与 [userId] 的连接并释放相关资源
     */
    suspend fun disconnect(userId: String)

    /**
     * 断开所有活跃连接
     */
    suspend fun disconnectAll()
}