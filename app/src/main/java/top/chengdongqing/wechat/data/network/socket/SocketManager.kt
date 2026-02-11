package top.chengdongqing.wechat.data.network.socket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TCP Socket 连接管理器
 */
@Singleton
class SocketManager @Inject constructor() {

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
        port: Int
    ): Result<SocketConnection> = withContext(Dispatchers.IO) {
        runCatching {
            // 如果已有连接，先关闭
            activeConnections[userId]?.close()

            Log.d(TAG, "正在连接: $host:$port")

            val socket = Socket()
            socket.connect(java.net.InetSocketAddress(host, port), SOCKET_TIMEOUT)
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
    suspend fun send(
        userId: String,
        data: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = activeConnections[userId]
                ?: throw Exception("未找到连接: $userId")

            synchronized(connection.outputStream) {
                // 先发送数据长度
                connection.outputStream.writeInt(data.size)
                // 再发送数据
                connection.outputStream.write(data)
                connection.outputStream.flush()
            }

            Log.d(TAG, "发送数据: $userId, ${data.size} 字节")

            Unit
        }.onFailure { error ->
            Log.e(TAG, "发送失败: $userId", error)
            disconnect(userId)
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
        return activeConnections[userId]?.socket?.isConnected == true
    }

    /**
     * 启动接收消息
     */
    private fun startReceiving(connection: SocketConnection) {
        scope.launch {
            try {
                while (connection.socket.isConnected && !connection.socket.isClosed) {
                    try {
                        // 读取数据长度
                        val length = connection.inputStream.readInt()

                        if (length <= 0 || length > 10 * 1024 * 1024) {  // 最大 10MB
                            Log.w(TAG, "异常数据长度: $length")
                            break
                        }

                        // 读取数据
                        val data = ByteArray(length)
                        connection.inputStream.readFully(data)

                        Log.d(TAG, "收到数据: ${connection.userId}, $length 字节")

                        // 发送到接收通道
                        connection.receiveChannel.send(data)

                    } catch (e: SocketTimeoutException) {
                        // 超时是正常的，继续读取
                        continue
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "接收异常: ${connection.userId}", e)
            } finally {
                disconnect(connection.userId)
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
                    kotlinx.coroutines.delay(HEARTBEAT_INTERVAL)

                    // 发送心跳包
                    val heartbeat = "PING".toByteArray()
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
 * Socket 连接
 */
data class SocketConnection(
    val userId: String,
    val socket: Socket,
    val inputStream: DataInputStream,
    val outputStream: DataOutputStream,
    val receiveChannel: Channel<ByteArray> = Channel(Channel.UNLIMITED)
) {
    fun close() {
        runCatching {
            receiveChannel.close()
            inputStream.close()
            outputStream.close()
            socket.close()
        }
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