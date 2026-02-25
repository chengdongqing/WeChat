package top.chengdongqing.wechat.data.network.socket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.connection.ConnectionManager
import top.chengdongqing.wechat.data.network.connection.PeerConnection
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.data.network.messaging.MessageReceiver
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketReader
import top.chengdongqing.wechat.data.network.protocol.PacketType
import top.chengdongqing.wechat.data.network.protocol.PacketWriter
import java.io.EOFException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TCP 入站连接管理器
 *
 * 监听随机端口，接受对方 [SocketClient] 的入站连接。
 * 每条连接独立协程处理：握手验证 → E2E 密钥交换 → 收包循环。
 *
 * 性能配置：
 * - Socket 收发缓冲区 512KB，跑满 LAN 带宽
 * - tcpNoDelay = true，禁用 Nagle 避免 40ms 延迟
 * - soTimeout = 0，由 Ping-Pong 判活
 */
@Singleton
class SocketServer @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val json: Json,
    private val e2e: E2ESessionManager,
    @param:IoScope private val scope: CoroutineScope
) {
    private companion object {
        const val TAG = "SocketServer"
    }

    private var serverSocket: ServerSocket? = null

    private val _incomingConnections = MutableSharedFlow<IncomingConnection>()

    /** 新入站连接事件流，[MessageReceiver] 订阅后自动启动包监听，以此解耦消息的接收 */
    val incomingConnections = _incomingConnections.asSharedFlow()

    // ==================== 生命周期 ====================

    /**
     * 启动监听，返回实际绑定的端口；失败返回 -1
     *
     * ServerSocket(0) 让系统分配随机端口，localPort 获取实际值。
     * receiveBufferSize 在 accept 前设置，子 socket 会自动继承。
     */
    suspend fun start(): Int = withContext(Dispatchers.IO) {
        try {
            val socket = ServerSocket(0).apply {
                receiveBufferSize = TransferConfig.SOCKET_RECV_BUFFER
            }
            serverSocket = socket
            scope.launch { acceptLoop() }
            Log.d(TAG, "服务端已启动，端口: ${socket.localPort}")
            socket.localPort
        } catch (e: Exception) {
            Log.e(TAG, "服务端启动失败", e)
            -1
        }
    }

    /** 停止监听，关闭所有入站连接 */
    fun stop() {
        serverSocket?.close()
        serverSocket = null
        scope.cancel()
        Log.d(TAG, "服务端已停止")
    }

    // ==================== 内部逻辑 ====================

    /** 循环 accept 新连接，每条连接启动独立协程处理 */
    private fun acceptLoop() {
        val socket = serverSocket ?: return
        while (!socket.isClosed) {
            try {
                val clientSocket = socket.accept()
                Log.d(TAG, "新连接: ${clientSocket.inetAddress.hostAddress}")
                scope.launch { handleClient(clientSocket) }
            } catch (e: Exception) {
                if (!socket.isClosed) Log.e(TAG, "接受连接异常", e)
            }
        }
    }

    /**
     * 处理新客户端连接
     *
     * 握手阶段设置短超时防慢连接攻击，握手完成后切换为无限阻塞由 Ping-Pong 判活。
     * 握手失败直接关闭 socket。
     */
    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            configureSocket(socket)
            val reader = PacketReader(socket.getInputStream())
            val writer = PacketWriter(socket.getOutputStream())

            socket.soTimeout = TransferConfig.HANDSHAKE_TIMEOUT  // 握手阶段：短超时防慢连接
            val userId = performHandshake(reader, writer) ?: run {
                Log.w(TAG, "握手失败，关闭连接")
                socket.close()
                return@withContext
            }
            socket.soTimeout = 0  // 通信阶段：无限阻塞，由 Ping-Pong 判活

            val connection = PeerConnection(userId, socket, reader, writer)
            connectionManager.register(connection)
            _incomingConnections.emit(IncomingConnection(userId, connection))
            Log.d(TAG, "客户端已连接: $userId")

            receiveLoop(connection)
        } catch (e: Exception) {
            Log.e(TAG, "处理客户端失败", e)
            socket.close()
        }
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
                Log.d(TAG, "E2E 握手 ACK 已回传: ${hs.senderId}")
            }

            hs.senderId
        } catch (e: Exception) {
            Log.e(TAG, "握手解析异常", e)
            null
        }
    }

    /**
     * 收包循环
     *
     * PING → 回 PONG
     * PONG → 忽略（服务端不发 Ping）
     * 其他 → 解密后推入 receiveChannel；body 为空说明解密失败，丢弃
     *
     * EOFException 表示对端正常关闭，其他异常打 error 日志。
     * finally 统一清理 E2E session 和连接记录。
     */
    private suspend fun receiveLoop(connection: PeerConnection) {
        try {
            while (connection.isActive) {
                val raw = connection.reader.read()
                when (raw.type) {
                    PacketType.PING -> connection.writer.write(Packet.pong())
                    PacketType.PONG -> {}
                    else -> {
                        val packet = e2e.decryptPacket(connection.userId, raw)
                        if (packet.body.isNotEmpty()) {
                            connection.receiveChannel.send(packet)
                        } else {
                            Log.w(TAG, "解密后 body 为空，丢弃: ${connection.userId}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is EOFException, is SocketException -> {
                    Log.d(TAG, "接收中断: ${connection.userId}, ${e.message}")
                }

                else -> Log.e(TAG, "接收中断: ${connection.userId}", e)
            }
        } finally {
            e2e.removeSession(connection.userId)
            cleanupConnection(connection.userId)
        }
    }

    private fun cleanupConnection(userId: String) {
        connectionManager.close(userId)
        Log.d(TAG, "连接已清理: $userId")
    }
}

/** 新入站连接通知，携带 userId 和连接实例供 [MessageReceiver] 订阅 */
data class IncomingConnection(
    val userId: String,
    val connection: PeerConnection
)