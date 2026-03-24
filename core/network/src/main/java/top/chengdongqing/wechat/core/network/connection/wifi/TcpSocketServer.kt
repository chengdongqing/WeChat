package top.chengdongqing.wechat.core.network.connection.wifi

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.common.di.IoScope
import top.chengdongqing.wechat.core.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.core.database.entity.ConnectionInfoEntity
import top.chengdongqing.wechat.core.network.config.TransferConfig
import top.chengdongqing.wechat.core.network.connection.ConnectionEvent
import top.chengdongqing.wechat.core.network.connection.ConnectionManager
import top.chengdongqing.wechat.core.network.connection.PeerConnection
import top.chengdongqing.wechat.core.network.connection.PeerHandshakeHandler
import top.chengdongqing.wechat.core.network.model.PacketReader
import top.chengdongqing.wechat.core.network.model.PacketWriter
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TCP 入站连接管理器
 */
@Singleton
class TcpSocketServer @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val handshakeHandler: PeerHandshakeHandler,
    private val connectionInfoDao: ConnectionInfoDao,
    @param:IoScope private val scope: CoroutineScope
) {
    private companion object {
        const val TAG = "TcpSocketServer"
    }

    private var serverSocket: ServerSocket? = null

    /**
     * 启动监听，返回实际绑定的端口；失败返回 -1
     *
     * ServerSocket(0) 让系统分配随机端口，localPort 获取实际值。
     * receiveBufferSize 在 accept 前设置，子 socket 会自动继承。
     */
    suspend fun start(port: Int = 0): Int = withContext(Dispatchers.IO) {
        try {
            val socket = ServerSocket(port).apply {
                receiveBufferSize = TransferConfig.SOCKET_RECV_BUFFER
            }
            serverSocket = socket
            scope.launch { acceptLoop() }
            socket.localPort
        } catch (e: Exception) {
            Log.e(TAG, "服务端启动失败", e)
            -1
        }
    }

    /**
     * 停止监听，关闭所有入站连接
     */
    fun stop() {
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (_: Exception) {
            Log.d(TAG, "Socket 服务已关闭")
        }
    }

    /**
     * 处理新连接
     */
    private fun acceptLoop() {
        val socket = serverSocket ?: return
        while (!socket.isClosed) {
            try {
                val clientSocket = socket.accept()
                scope.launch { handleClient(clientSocket) }
            } catch (e: Exception) {
                if (!socket.isClosed) {
                    Log.e(TAG, "接受连接异常", e)
                }
            }
        }
    }

    /**
     * 处理新客户端连接
     *
     * 握手阶段设置短超时防慢连接攻击，握手完成后切换为无限阻塞由 Ping-Pong 判活。
     */
    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            // 配置socket
            configureSocket(socket)
            // 构建reader和writer
            val reader = PacketReader(socket.getInputStream())
            val writer = PacketWriter(socket.getOutputStream())
            // 短超时防慢连接攻击
            socket.soTimeout = TransferConfig.HANDSHAKE_TIMEOUT

            // 执行握手
            val userId = handshakeHandler.acceptHandshake(reader, writer) ?: run {
                Log.w(TAG, "握手失败，关闭连接")
                socket.close()
                return@withContext
            }
            // 取消超时限制
            socket.soTimeout = 0

            val conn = PeerConnection(
                userId = userId,
                reader = reader,
                writer = writer,
                isActiveProvider = { socket.isConnected && !socket.isClosed },
                closeAction = { socket.close() }
            )
            // 保存连接
            connectionManager.register(conn)
            // 推送连接事件
            connectionManager.emitEvent(ConnectionEvent.Connected(userId, conn))

            // 开始接收数据
            connectionManager.startReceiving(conn)
            // 开始维持心跳
            connectionManager.startHeartbeat(conn)

            // 保存连接信息到数据库
            saveToDB(userId, socket)
        } catch (e: Exception) {
            Log.e(TAG, "处理客户端失败", e)
            socket.close()
        }
    }

    /**
     * 配置 Socket 参数
     */
    private fun configureSocket(socket: Socket) {
        socket.sendBufferSize = TransferConfig.SOCKET_SEND_BUFFER
        socket.receiveBufferSize = TransferConfig.SOCKET_RECV_BUFFER
        socket.keepAlive = true
        socket.tcpNoDelay = true // 禁用 Nagle，避免与 Delayed ACK 叠加产生 40ms 延迟。
    }

    /**
     * 保存连接信息到数据库
     */
    suspend fun saveToDB(userId: String, socket: Socket) {
        connectionInfoDao.upsert(
            ConnectionInfoEntity(
                userId = userId,
                lanIpAddress = socket.inetAddress.hostAddress,
                lanPort = socket.port,
                isOnline = true,
                lastSeen = System.currentTimeMillis()
            )
        )
    }
}