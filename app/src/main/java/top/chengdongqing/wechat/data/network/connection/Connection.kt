package top.chengdongqing.wechat.data.network.connection

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.network.protocol.P2PMessage

/**
 * 连接抽象接口
 */
interface Connection {

    /**
     * 远程用户ID
     */
    val remoteUserId: String

    /**
     * 连接类型
     */
    val type: ConnectionType

    /**
     * 是否活跃
     */
    fun isActive(): Boolean

    /**
     * 发送消息
     */
    suspend fun send(message: P2PMessage)

    /**
     * 接收消息流
     */
    fun messageFlow(): Flow<P2PMessage>

    /**
     * 关闭连接
     */
    fun close()
}

enum class ConnectionType {
    BLUETOOTH,
    WIFI_LAN,
    WIFI_DIRECT
}