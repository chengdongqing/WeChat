package top.chengdongqing.wechat.data.network.socket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketReader
import top.chengdongqing.wechat.data.network.protocol.PacketType
import top.chengdongqing.wechat.data.network.protocol.PacketWriter
import java.io.EOFException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TCP 服务器（被动端）
 *
 * 接受连接 → HANDSHAKE 握手 → 收包循环（PING→PONG / 其他→Channel）
 *
 * 性能配置:
 * - Socket 收发缓冲区 512KB（跑满 LAN 带宽）
 * - soTimeout = 0，由 Ping-Pong 判活
 * - tcpNoDelay = true，禁用 Nagle 避免 40ms 延迟惩罚
 */
@Singleton
class SocketServer @Inject constructor(
    private val json: Json,
    private val e2e: E2ESessionManager
) {
    private companion object {
        const val TAG = "SocketServer"
    }

    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _incomingConnections = MutableSharedFlow<IncomingConnection>()
    val incomingConnections: SharedFlow<IncomingConnection> = _incomingConnections.asSharedFlow()

    private val activeClients = ConcurrentHashMap<String, ClientConnection>()

    // ==================== 生命周期 ====================

    suspend fun start(): Int = withContext(Dispatchers.IO) {
        try {
            val socket = ServerSocket(0).apply {
                // 设置 server socket 的接收缓冲区，会被 accept 出的子 socket 继承
                receiveBufferSize = TransferConfig.SOCKET_RECV_BUFFER
            }
            serverSocket = socket
            val port = socket.localPort
            Log.d(TAG, "服务器已启动，端口: $port")

            scope.launch { acceptLoop() }
            port
        } catch (e: Exception) {
            Log.e(TAG, "服务器启动失败", e)
            -1
        }
    }

    fun stop() {
        activeClients.values.forEach { it.close() }
        activeClients.clear()
        serverSocket?.close()
        serverSocket = null
        scope.cancel()
        Log.d(TAG, "服务器已停止")
    }

    // ==================== 发送 ====================

    suspend fun sendToClient(userId: String, packet: Packet): Result<Unit> {
        val connection = activeClients[userId]
            ?: return Result.failure(IllegalStateException("客户端未连接: $userId"))

        return withContext(Dispatchers.IO) {
            runCatching { connection.writer.write(encryptIfNeeded(userId, packet)) }
        }
    }

    // ==================== 内部逻辑 ====================

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

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            configureSocket(socket)

            val reader = PacketReader(socket.getInputStream())
            val writer = PacketWriter(socket.getOutputStream())

            // 握手阶段: 短超时防慢连接
            socket.soTimeout = TransferConfig.HANDSHAKE_TIMEOUT

            val userId = performHandshake(reader, writer) ?: run {
                Log.w(TAG, "握手失败，关闭连接")
                socket.close()
                return@withContext
            }

            // 通信阶段: 无限阻塞，由 Ping-Pong 判活
            socket.soTimeout = 0

            val connection = ClientConnection(userId, socket, reader, writer)
            activeClients[userId] = connection

            _incomingConnections.emit(IncomingConnection(userId, connection))
            Log.d(TAG, "客户端已连接: $userId")

            receiveLoop(connection)
        } catch (e: Exception) {
            Log.e(TAG, "处理客户端失败", e)
            socket.close()
        }
    }

    /**
     * 配置 Socket 参数以跑满 LAN 带宽
     */
    private fun configureSocket(socket: Socket) {
        socket.sendBufferSize = TransferConfig.SOCKET_SEND_BUFFER
        socket.receiveBufferSize = TransferConfig.SOCKET_RECV_BUFFER
        socket.keepAlive = true
        socket.tcpNoDelay = true    // 禁用 Nagle，避免与 Delayed ACK 叠加导致 40ms 延迟
    }

    private fun performHandshake(reader: PacketReader, writer: PacketWriter): String? {
        return try {
            val packet = reader.read()
            if (packet.type != PacketType.HANDSHAKE) {
                Log.w(TAG, "握手包类型错误: ${packet.type}")
                return null
            }

            val handshake = json.decodeFromString<ChatProtocol.Handshake>(
                String(packet.body, Charsets.UTF_8)
            )

            // E2E：若对方携带公钥，立即响应
            handshake.e2ePublicKey?.let { peerKey ->
                val myKey = e2e.acceptHandshake(handshake.senderId, peerKey)
                val ack =
                    ChatProtocol.Handshake(senderId = handshake.senderId, e2ePublicKeyAck = myKey)
                val body = json.encodeToString<ChatProtocol>(ack).toByteArray(Charsets.UTF_8)
                writer.write(Packet(PacketType.HANDSHAKE, body))
                Log.d(TAG, "E2E 握手 ACK 已发送 (server): ${handshake.senderId}")
            }

            handshake.senderId
        } catch (e: Exception) {
            Log.e(TAG, "握手解析异常", e)
            null
        }
    }

    private suspend fun receiveLoop(connection: ClientConnection) {
        try {
            while (connection.isActive) {
                val raw = connection.reader.read()
                Log.d(
                    TAG,
                    "📦 收到包: type=0x${raw.type.toString(16)} size=${raw.body.size} from=${connection.userId}"
                )

                when (raw.type) {
                    PacketType.PING -> connection.writer.write(Packet.pong())
                    PacketType.PONG -> {} // 忽略

                    else -> {
                        val isEnc = PacketType.isEncrypted(raw.type)
                        Log.d(TAG, "📨 转发包: type=0x${raw.type.toString(16)} encrypted=$isEnc")
                        val packet = decryptIfNeeded(connection.userId, raw)
                        if (packet.body.isNotEmpty()) {
                            connection.receiveChannel.send(packet)
                        } else {
                            Log.w(TAG, "⚠️ 解密后 body 为空，丢弃: ${connection.userId}")
                        }
                    }
                }
            }
        } catch (_: EOFException) {
            Log.d(TAG, "客户端正常断开: ${connection.userId}")
        } catch (e: Exception) {
            Log.e(TAG, "接收中断: ${connection.userId}", e)
        } finally {
            e2e.removeSession(connection.userId)   // 连接断开，清理 session
            cleanupConnection(connection.userId)
        }
    }

    private fun cleanupConnection(userId: String) {
        activeClients.remove(userId)?.close()
        Log.d(TAG, "连接已清理: $userId")
    }

    private fun encryptIfNeeded(peerId: String, packet: Packet): Packet {
        if (packet.type in PacketType.PLAINTEXT_TYPES) return packet
        if (!e2e.hasSession(peerId)) return packet
        return runCatching {
            Packet(PacketType.encryptedType(packet.type), e2e.encrypt(peerId, packet.body))
        }.getOrElse { packet }
    }

    private fun decryptIfNeeded(peerId: String, packet: Packet): Packet {
        if (!PacketType.isEncrypted(packet.type)) return packet
        val baseType = PacketType.realType(packet.type)
        if (!e2e.hasSession(peerId)) return Packet(baseType, ByteArray(0))
        return runCatching {
            Packet(baseType, e2e.decrypt(peerId, packet.body))
        }.getOrElse { Packet(baseType, ByteArray(0)) }
    }
}

// ==================== 数据类 ====================

data class ClientConnection(
    val userId: String,
    val socket: Socket,
    val reader: PacketReader,
    val writer: PacketWriter,
    val receiveChannel: Channel<Packet> = Channel(Channel.UNLIMITED)
) {
    val isActive: Boolean get() = socket.isConnected && !socket.isClosed

    fun close() {
        runCatching {
            receiveChannel.close()
            reader.close()
            writer.close()
            socket.close()
        }
    }
}

data class IncomingConnection(
    val userId: String,
    val connection: ClientConnection
)