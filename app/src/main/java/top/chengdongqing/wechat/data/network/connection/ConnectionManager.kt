package top.chengdongqing.wechat.data.network.connection

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import top.chengdongqing.wechat.core.designsystem.util.isTrue
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 连接管理器
 */
@Singleton
class ConnectionManager @Inject constructor() {

    // Socket 连接池
    private val connections = ConcurrentHashMap<String, PeerConnection>()

    // 连接状态事件流
    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>()
    val connectionEvents = _connectionEvents.asSharedFlow()

    /**
     * 获取指定连接
     */
    fun getConnection(userId: String) = connections[userId]

    /**
     * 获取指定连接
     * 连接不存在时抛出异常
     */
    fun requireConnection(userId: String) =
        getConnection(userId) ?: throw IllegalStateException("未找到连接: $userId")

    /**
     * 是否已连接
     */
    fun isConnected(userId: String): Boolean = getConnection(userId)?.isActive.isTrue()

    /**
     * 注册连接
     */
    fun register(conn: PeerConnection) {
        // 关闭可能已存在的连接
        connections[conn.userId]?.close()
        // 保存新连接
        connections[conn.userId] = conn
    }

    /**
     * 关闭指定连接
     */
    fun close(userId: String) {
        connections.remove(userId)?.close()
    }

    /**
     * 关闭所有连接
     */
    fun closeAll() {
        connections.values.forEach { it.close() }
        connections.clear()
    }

    /**
     * 推送连接事件
     */
    suspend fun emitEvent(event: ConnectionEvent) {
        _connectionEvents.emit(event)
    }
}

/**
 * 连接事件
 */
sealed class ConnectionEvent {
    /**
     * TCP 连接已建立并完成握手
     */
    data class Connected(val userId: String, val conn: PeerConnection) : ConnectionEvent()

    /**
     * 连接已断开
     *
     * @param reason 说明原因（主动断开 / Pong 超时 / 接收异常等）
     */
    data class Disconnected(val userId: String, val reason: String?) : ConnectionEvent()
}