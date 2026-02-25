package top.chengdongqing.wechat.data.network.connection

import top.chengdongqing.wechat.core.designsystem.util.isTrue
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 连接管理器
 */
@Singleton
class ConnectionManager @Inject constructor() {
    private val connections = ConcurrentHashMap<String, PeerConnection>()

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
}