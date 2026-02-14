package top.chengdongqing.wechat.data.network.socket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.EOFException
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.designsystem.util.isTrue
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.SocketFrame
import top.chengdongqing.wechat.data.network.protocol.SocketProtocol.TYPE_BINARY
import top.chengdongqing.wechat.data.network.protocol.SocketProtocol.TYPE_JSON
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TCP Socket 连接管理器
 */
@Singleton
class SocketManager @Inject constructor(
    private val json: Json
) {

    private companion object {
        const val TAG = "SocketManager"
        const val SOCKET_TIMEOUT = 30000  // 30秒超时
        const val HEARTBEAT_INTERVAL = 15000L  // 15秒心跳
    }

    // 存储活跃的连接
    private val activeConnections = mutableMapOf<String, SocketConnection>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>()
    val connectionEvents: Flow<ConnectionEvent> = _connectionEvents.asSharedFlow()

    /**
     * 连接到对方
     */
    suspend fun connect(
        userId: String,
        host: String,
        port: Int,
        myUserId: String
    ): Result<SocketConnection> = withContext(Dispatchers.IO) {
        runCatching {
            // 如果已有连接，先关闭
            activeConnections[userId]?.close()

            Log.d("DEBUG", "我是主动连接方 -> $myUserId 连接 $userId")
            Log.d(TAG, "正在连接: $host:$port")

            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), SOCKET_TIMEOUT)
            socket.soTimeout = SOCKET_TIMEOUT
            socket.keepAlive = true
            socket.tcpNoDelay = true  // 禁用 Nagle 算法，降低延迟

            val connection = SocketConnection(
                userId = userId,
                socket = socket,
                inputStream = DataInputStream(socket.getInputStream()),
                outputStream = DataOutputStream(socket.getOutputStream())
            )

            activeConnections[userId] = connection

            // 连接后立即发送心跳包
            val heartbeat = json.encodeToString<ChatProtocol>(
                ChatProtocol.Heartbeat("", myUserId)
            ).toByteArray(Charsets.UTF_8)

            synchronized(connection.outputStream) {
                connection.outputStream.writeByte(TYPE_JSON.toInt())
                connection.outputStream.writeInt(heartbeat.size)
                connection.outputStream.write(heartbeat)
                connection.outputStream.flush()
            }

            Log.d(TAG, "✅ 心跳包已发送")

            // 启动接收消息的协程
            startReceiving(connection)
            // 启动心跳
            startHeartbeat(connection)

            _connectionEvents.emit(ConnectionEvent.Connected(userId))

            Log.d(TAG, "✅ 连接成功: $userId")
            connection
        }.onFailure { error ->
            Log.e(TAG, "连接失败: $userId", error)
            _connectionEvents.emit(ConnectionEvent.Disconnected(userId, error.message))
        }
    }

    /**
     * 发送数据
     */
    /**
     * 发送 JSON 协议消息
     */
    suspend fun send(userId: String, data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = activeConnections[userId]
                ?: throw Exception("未找到连接: $userId")

            synchronized(connection.outputStream) {
                connection.outputStream.writeByte(TYPE_JSON.toInt())
                connection.outputStream.writeInt(data.size)
                connection.outputStream.write(data)
                connection.outputStream.flush()
            }
        }
    }

    /**
     * 发送原始二进制块
     */
    suspend fun sendBinary(
        userId: String,
        messageId: String,
        data: ByteArray,
        offset: Int,
        length: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = activeConnections[userId]
                ?: throw Exception("未找到连接: $userId")

            val idBytes = messageId.toByteArray(Charsets.UTF_8)

            synchronized(connection.outputStream) {
                connection.outputStream.writeByte(TYPE_BINARY.toInt())
                connection.outputStream.writeByte(idBytes.size)      // messageId 长度
                connection.outputStream.write(idBytes)               // messageId
                connection.outputStream.writeInt(length)
                connection.outputStream.write(data, offset, length)
                connection.outputStream.flush()
            }
        }
    }

    /**
     * 断开连接
     */
    suspend fun disconnect(userId: String) {
        withContext(Dispatchers.IO) {
            activeConnections.remove(userId)?.close()
            _connectionEvents.emit(ConnectionEvent.Disconnected(userId, "主动断开"))
            Log.d(TAG, "已断开: $userId")
        }
    }

    /**
     * 获取连接
     */
    fun getConnection(userId: String): SocketConnection? {
        return activeConnections[userId]
    }

    /**
     * 是否已连接
     */
    fun isConnected(userId: String): Boolean {
        return activeConnections[userId]?.socket?.isConnected.isTrue()
    }

    /**
     * 启动接收消息
     */
    fun startReceiving(connection: SocketConnection) {
        scope.launch {
            val inputStream = connection.inputStream
            try {
                while (isActive) {
                    // 1. 读取类型 (1 字节) - 这里如果读不到就会抛 EOFException，正常断开应捕获
                    val type = try {
                        inputStream.readByte()
                    } catch (_: EOFException) {
                        break
                    }

                    // 2. 读取长度 (4 字节)
                    val length = inputStream.readInt()

                    // 3. 读取内容
                    val data = ByteArray(length)
                    inputStream.readFully(data) // 必须用 readFully 确保读够长度

                    // 4. 分发到对应通道
                    when (type) {
                        TYPE_JSON -> connection.frameChannel.send(
                            SocketFrame.JsonFrame(data)
                        )

                        TYPE_BINARY -> {
                            val idLength = inputStream.readByte().toInt()
                            val idBytes = ByteArray(idLength)
                            inputStream.readFully(idBytes)
                            val messageId = String(idBytes, Charsets.UTF_8)

                            val dataLength = inputStream.readInt()
                            val frameData = ByteArray(dataLength)
                            inputStream.readFully(frameData)

                            connection.frameChannel.send(
                                SocketFrame.BinaryFrame(messageId, frameData)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SocketManager", "接收异常: ${connection.userId}", e)
            } finally {
                disconnect(connection.userId) // 发生错误或读完则清理连接
            }
        }
    }

    /**
     * 启动心跳
     */
    private fun startHeartbeat(connection: SocketConnection) {
        scope.launch {
            try {
                while (connection.socket.isConnected && !connection.socket.isClosed) {
                    delay(HEARTBEAT_INTERVAL)
                    val protocol = ChatProtocol.Heartbeat("", connection.userId)

                    // 发送心跳包
                    val heartbeat = Json.encodeToString<ChatProtocol>(protocol)
                        .toByteArray(Charsets.UTF_8)
                    send(connection.userId, heartbeat)
                }
            } catch (e: Exception) {
                Log.e(TAG, "心跳异常: ${connection.userId}", e)
            }
        }
    }

    /**
     * 关闭所有连接
     */
    fun closeAll() {
        activeConnections.values.forEach { it.close() }
        activeConnections.clear()
    }
}

/**
 * 连接事件
 */
sealed class ConnectionEvent {
    data class Connected(val userId: String) : ConnectionEvent()
    data class Disconnected(val userId: String, val reason: String?) : ConnectionEvent()
    data class Error(val userId: String, val error: String) : ConnectionEvent()
}