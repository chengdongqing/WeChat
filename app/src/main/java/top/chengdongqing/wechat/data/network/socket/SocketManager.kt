package top.chengdongqing.wechat.data.network.socket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketReader
import top.chengdongqing.wechat.data.network.protocol.PacketType
import top.chengdongqing.wechat.data.network.protocol.PacketWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TCP 客户端连接管理器（主动端）
 *
 * 性能设计:
 * - Socket 收发缓冲区 512KB，匹配 LAN 带宽
 * - BufferedOutputStream 256KB，合并写入减少 syscall
 * - tcpNoDelay = true，禁用 Nagle
 * - 文件传输通过 [sendAtomicTransfer] 持有 Mutex，帧序列原子
 * - 心跳引用计数，传输中自动暂停
 */
@Singleton
class SocketManager @Inject constructor(
    private val json: Json
) {
    private companion object {
        const val TAG = "SocketManager"
    }

    private val activeConnections = ConcurrentHashMap<String, SocketConnection>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>()
    val connectionEvents: Flow<ConnectionEvent> = _connectionEvents.asSharedFlow()

    // ==================== 公开接口 ====================

    suspend fun connect(
        userId: String,
        host: String,
        port: Int,
        myUserId: String
    ): Result<SocketConnection> = withContext(Dispatchers.IO) {
        runCatching {
            closeExisting(userId)
            Log.d(TAG, "正在连接: $host:$port")

            val socket = createSocket(host, port)
            val connection = SocketConnection(
                userId = userId,
                socket = socket,
                reader = PacketReader(socket.getInputStream()),
                writer = PacketWriter(socket.getOutputStream())
            )
            activeConnections[userId] = connection

            sendHandshake(connection, myUserId)
            startReceiving(connection)
            startHeartbeat(connection)

            _connectionEvents.emit(ConnectionEvent.Connected(userId))
            Log.d(TAG, "连接成功: $userId")
            connection
        }.onFailure { error ->
            Log.e(TAG, "连接失败: $userId", error)
            _connectionEvents.emit(ConnectionEvent.Disconnected(userId, error.message))
        }
    }

    /** 发送单个 Packet 并立即 flush */
    suspend fun send(userId: String, packet: Packet): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireConnection(userId).writer.write(packet)
            }.onFailure { error ->
                Log.e(TAG, "发送失败: $userId", error)
                disconnect(userId)
            }
        }

    /**
     * 原子发送一组 Packet（文件传输专用）
     *
     * Mutex 保证帧序列不被其他传输打断。
     * block 接收 [PacketWriter]，可使用 writeNoFlush + 手动 flush
     * 来减少 syscall（BufferedOutputStream 在 buffer 满时也会自动 flush）。
     */
    suspend fun sendAtomicTransfer(
        userId: String,
        block: suspend (PacketWriter) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val conn = activeConnections[userId]
            ?: return@withContext Result.failure(IllegalStateException("未找到连接: $userId"))

        conn.transferMutex.lock()
        conn.incrementTransferCount()
        try {
            block(conn.writer)
            conn.writer.flush()     // 确保最后残留的数据刷出
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "原子传输失败: $userId", e)
            disconnect(userId)
            Result.failure(e)
        } finally {
            conn.decrementTransferCount()
            conn.transferMutex.unlock()
        }
    }

    suspend fun disconnect(userId: String) {
        withContext(Dispatchers.IO) {
            activeConnections.remove(userId)?.close()
            _connectionEvents.emit(ConnectionEvent.Disconnected(userId, "主动断开"))
            Log.d(TAG, "已断开: $userId")
        }
    }

    fun getConnection(userId: String): SocketConnection? = activeConnections[userId]

    fun isConnected(userId: String): Boolean =
        activeConnections[userId]?.isActive == true

    fun closeAll() {
        activeConnections.values.forEach { it.close() }
        activeConnections.clear()
    }

    // ==================== 内部逻辑 ====================

    private fun requireConnection(userId: String): SocketConnection =
        activeConnections[userId]
            ?: throw IllegalStateException("未找到连接: $userId")

    /**
     * 创建并配置 Socket
     *
     * 关键参数:
     * - sendBufferSize / receiveBufferSize = 512KB → 充足的 TCP 窗口
     * - tcpNoDelay = true → 禁用 Nagle，配合我们自己的 BufferedOutputStream 做批量写入
     * - soTimeout = 0 → 由 Ping-Pong 判活，不依赖读超时
     */
    private fun createSocket(host: String, port: Int): Socket =
        Socket().apply {
            // 注: send/receiveBufferSize 需要在 connect 之前设置才能影响 TCP 窗口协商
            sendBufferSize = TransferConfig.SOCKET_SEND_BUFFER
            receiveBufferSize = TransferConfig.SOCKET_RECV_BUFFER
            connect(InetSocketAddress(host, port), TransferConfig.CONNECT_TIMEOUT)
            soTimeout = 0
            keepAlive = true
            tcpNoDelay = true
        }

    private fun sendHandshake(connection: SocketConnection, myUserId: String) {
        val body = json.encodeToString<ChatProtocol>(
            ChatProtocol.Heartbeat(senderId = myUserId)
        ).toByteArray(Charsets.UTF_8)

        connection.writer.write(Packet(PacketType.HANDSHAKE, body))
        Log.d(TAG, "握手包已发送")
    }

    private fun closeExisting(userId: String) {
        activeConnections.remove(userId)?.close()
    }

    private fun startReceiving(connection: SocketConnection) {
        scope.launch {
            try {
                while (connection.isActive) {
                    val packet = connection.reader.read()

                    when (packet.type) {
                        PacketType.PONG -> connection.lastPongTime.set(System.currentTimeMillis())
                        PacketType.PING -> connection.writer.write(Packet.pong())
                        else -> connection.receiveChannel.send(packet)
                    }
                }
            } catch (e: Exception) {
                if (connection.isActive) {
                    Log.e(TAG, "接收异常: ${connection.userId}", e)
                }
            } finally {
                disconnect(connection.userId)
            }
        }
    }

    private fun startHeartbeat(connection: SocketConnection) {
        connection.lastPongTime.set(System.currentTimeMillis())

        connection.heartbeatJob = scope.launch {
            try {
                while (connection.isActive) {
                    delay(TransferConfig.PING_INTERVAL)

                    if (connection.activeTransferCount.get() > 0) continue

                    val elapsed = System.currentTimeMillis() - connection.lastPongTime.get()
                    if (elapsed > TransferConfig.PONG_TIMEOUT) {
                        Log.w(TAG, "Pong 超时 (${elapsed}ms)，断开: ${connection.userId}")
                        disconnect(connection.userId)
                        break
                    }

                    runCatching {
                        connection.writer.write(Packet.ping())
                    }.onFailure {
                        Log.e(TAG, "Ping 失败: ${connection.userId}", it)
                        disconnect(connection.userId)
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "心跳异常: ${connection.userId}", e)
            }
        }
    }
}

// ==================== 数据类 ====================

data class SocketConnection(
    val userId: String,
    val socket: Socket,
    val reader: PacketReader,
    val writer: PacketWriter,
    val receiveChannel: Channel<Packet> = Channel(Channel.UNLIMITED),
    val transferMutex: Mutex = Mutex(),
    val activeTransferCount: AtomicInteger = AtomicInteger(0),
    val lastPongTime: AtomicLong = AtomicLong(System.currentTimeMillis()),
    var heartbeatJob: Job? = null
) {
    val isActive: Boolean get() = socket.isConnected && !socket.isClosed

    fun incrementTransferCount() {
        activeTransferCount.incrementAndGet()
    }

    fun decrementTransferCount() {
        if (activeTransferCount.decrementAndGet() <= 0) {
            lastPongTime.set(System.currentTimeMillis())
        }
    }

    fun close() {
        runCatching {
            heartbeatJob?.cancel()
            receiveChannel.close()
            reader.close()
            writer.close()
            socket.close()
        }
    }
}

sealed class ConnectionEvent {
    data class Connected(val userId: String) : ConnectionEvent()
    data class Disconnected(val userId: String, val reason: String?) : ConnectionEvent()
}