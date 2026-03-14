package top.chengdongqing.wechat.data.network.connection.wifi

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.entity.ConnectionInfoEntity
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.connection.ConnectionEvent
import top.chengdongqing.wechat.data.network.connection.ConnectionMode
import top.chengdongqing.wechat.data.network.connection.PeerConnection
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketReader
import top.chengdongqing.wechat.data.network.protocol.PacketType
import top.chengdongqing.wechat.data.network.protocol.PacketWriter
import top.chengdongqing.wechat.features.settings.domain.repository.ConnectionSettingsRepository
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TCP 入站连接管理器
 *
 * 监听随机端口，接受对方 [SocketClient] 的入站连接。
 * 每条连接独立协程处理：握手验证 → E2E 密钥交换 → 收包循环。
 */
@Singleton
class SocketServer @Inject constructor(
    private val json: Json,
    private val e2e: E2ESessionManager,
    private val connectionManager: ConnectionManager,
    private val connectionInfoDao: ConnectionInfoDao,
    private val connectionSettingsRepository: ConnectionSettingsRepository,
    @param:IoScope private val scope: CoroutineScope
) {
    private companion object {
        const val TAG = "SocketServer"
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
            val userId = performHandshake(reader, writer) ?: run {
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

            val connectionMode = connectionSettingsRepository.connectionMode.first()
            if (connectionMode == ConnectionMode.WiFiDirect) {
                socket.inetAddress.hostAddress?.let { ip ->
                    saveConnectionInfo(userId, ip)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理客户端失败", e)
            socket.close()
        }
    }

    /**
     * 保存连接信息到数据库
     */
    private suspend fun saveConnectionInfo(userId: String, ip: String) {
        val info = ConnectionInfoEntity(
            userId = userId,
            p2pIpAddress = ip,
            p2pPort = 8888,
            isOnline = true,
            lastSeen = System.currentTimeMillis()
        )
        connectionInfoDao.insertOrUpdate(info)
        Log.d(TAG, "已保存连接信息: $info")
    }

    /**
     * 配置 Socket 参数
     *
     * tcpNoDelay = true：禁用 Nagle，避免与 Delayed ACK 叠加产生 40ms 延迟。
     */
    private fun configureSocket(socket: Socket) {
        socket.sendBufferSize = TransferConfig.SOCKET_SEND_BUFFER
        socket.receiveBufferSize = TransferConfig.SOCKET_RECV_BUFFER
        socket.keepAlive = true
        socket.tcpNoDelay = true
    }

    /**
     * 执行握手协议
     *
     * 读取第一个包，验证类型为 HANDSHAKE，解析 senderId。
     * 若携带 e2ePublicKey，作为被动方完成 E2E 密钥交换并立即回传 ACK。
     * 成功返回 senderId，失败返回 null。
     */
    private fun performHandshake(reader: PacketReader, writer: PacketWriter): String? {
        return try {
            val packet = reader.read()
            if (packet.type != PacketType.HANDSHAKE) {
                Log.w(TAG, "握手包类型错误: ${packet.type}")
                return null
            }

            val hs = json.decodeFromString<ChatProtocol.Handshake>(
                String(packet.body, Charsets.UTF_8)
            )

            hs.e2ePublicKey?.let { peerKey ->
                val myKey = e2e.acceptHandshake(hs.senderId, peerKey)
                val ack = ChatProtocol.Handshake(senderId = hs.senderId, e2ePublicKeyAck = myKey)
                writer.write(
                    Packet(
                        PacketType.HANDSHAKE,
                        json.encodeToString<ChatProtocol>(ack).toByteArray(Charsets.UTF_8)
                    )
                )
            }

            hs.senderId
        } catch (_: Exception) {
            null
        }
    }
}