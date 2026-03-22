package top.chengdongqing.wechat.data.network.connection.wifi

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.connection.ConnectionEvent
import top.chengdongqing.wechat.data.network.connection.ConnectionManager
import top.chengdongqing.wechat.data.network.connection.PeerConnection
import top.chengdongqing.wechat.data.network.connection.PeerHandshakeHandler
import top.chengdongqing.wechat.data.network.model.PacketReader
import top.chengdongqing.wechat.data.network.model.PacketWriter
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TCP 出站连接管理器
 *
 * 负责主动发起连接、E2E 握手、收包路由和心跳保活。
 */
@Singleton
class TcpSocketClient @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val handshakeHandler: PeerHandshakeHandler
) {
    private companion object {
        const val TAG = "TcpSocketClient"
    }

    /**
     * 主动连接指定用户
     *
     * 若已有旧连接先关闭，再建新连接。
     * 成功后依次：发握手 → 启动收包 → 启动心跳 → 发出 Connected 事件。
     */
    suspend fun connect(
        userId: String,
        host: String,
        port: Int,
        myUserId: String
    ): Result<PeerConnection> = withContext(Dispatchers.IO) {
        runCatching {
            val socket = createSocket(host, port)

            val conn = PeerConnection(
                userId = userId,
                reader = PacketReader(socket.getInputStream()),
                writer = PacketWriter(socket.getOutputStream()),
                isActiveProvider = { socket.isConnected && !socket.isClosed },
                closeAction = { socket.close() }
            )
            // 保存连接
            connectionManager.register(conn)
            // 推送连接成功事件
            connectionManager.emitEvent(ConnectionEvent.Connected(userId, conn))

            // 发出握手包（携带己方公钥）
            conn.writer.write(handshakeHandler.buildHandshakePacket(userId, myUserId))

            // 开始接收数据
            connectionManager.startReceiving(conn) { packet ->
                handshakeHandler.handleHandshakeReply(conn, packet)
            }
            // 开始维持心跳
            connectionManager.startHeartbeat(conn)

            conn
        }.onFailure { e ->
            Log.w(TAG, "连接失败: $userId, ${e.message}")
            connectionManager.emitEvent(ConnectionEvent.Disconnected(userId, e.message))
        }
    }

    /**
     * 创建并配置 Socket
     *
     * 缓冲区必须在 connect 前设置，才能影响 TCP 窗口协商。
     * soTimeout = 0：不用读超时，由 Ping-Pong 判断连接存活。
     */
    private fun createSocket(host: String, port: Int): Socket = Socket().apply {
        sendBufferSize = TransferConfig.SOCKET_SEND_BUFFER
        receiveBufferSize = TransferConfig.SOCKET_RECV_BUFFER

        connect(InetSocketAddress(host, port), TransferConfig.CONNECT_TIMEOUT)

        soTimeout = 0
        keepAlive = true
        tcpNoDelay = true
    }
}