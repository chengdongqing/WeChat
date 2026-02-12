package top.chengdongqing.wechat.data.network.socket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Socket 服务器（接受其他设备的连接）
 */
@Singleton
class SocketServer @Inject constructor(
    private val json: Json
) {

    private companion object {
        const val TAG = "SocketServer"
        const val SERVER_PORT = 8888
    }

    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _incomingConnections = MutableSharedFlow<IncomingConnection>()
    val incomingConnections: SharedFlow<IncomingConnection> = _incomingConnections.asSharedFlow()

    private val activeClients = mutableMapOf<String, ClientConnection>()

    /**
     * 启动服务器
     */
    fun start() {
        scope.launch {
            try {
                serverSocket = ServerSocket(SERVER_PORT)
                Log.d(TAG, "✅ 服务器已启动，监听端口: $SERVER_PORT")

                while (!serverSocket!!.isClosed) {
                    try {
                        // 等待客户端连接
                        val clientSocket = serverSocket!!.accept()
                        Log.d(TAG, "✅ 收到新连接: ${clientSocket.inetAddress.hostAddress}")
                        // 处理客户端连接
                        handleClient(clientSocket)
                    } catch (e: Exception) {
                        if (!serverSocket!!.isClosed) {
                            Log.e(TAG, "接受连接失败", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "服务器异常", e)
            }
        }
    }

    /**
     * 处理客户端连接
     */
    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                socket.soTimeout = 30000
                socket.keepAlive = true
                socket.tcpNoDelay = true

                val inputStream = DataInputStream(socket.getInputStream())
                val outputStream = DataOutputStream(socket.getOutputStream())

                // 读取握手包
                val length = inputStream.readInt()
                val data = ByteArray(length)
                inputStream.readFully(data)

                val jsonString = String(data, Charsets.UTF_8)
                val protocol = runCatching {
                    json.decodeFromString<ChatProtocol>(jsonString)
                }.getOrNull()

                // 必须是心跳包才接受连接
                val heartbeat = protocol as? ChatProtocol.Heartbeat
                if (heartbeat == null) {
                    Log.w(TAG, "❌ 握手失败，关闭连接")
                    socket.close()
                    return@launch
                }

                val userId = heartbeat.senderId
                Log.d(TAG, "✅ 握手成功: $userId")

                val connection = ClientConnection(
                    userId = userId,
                    socket = socket,
                    inputStream = inputStream,
                    outputStream = outputStream
                )

                activeClients[userId] = connection

                _incomingConnections.emit(
                    IncomingConnection(
                        userId = userId,
                        connection = connection
                    )
                )

                Log.d(TAG, "✅ 客户端已连接: $userId")

                // 持续接收消息
                receiveMessages(connection)
            } catch (e: Exception) {
                Log.e(TAG, "处理客户端失败", e)
                socket.close()
            }
        }
    }

    /**
     * 接收消息
     */
    private suspend fun receiveMessages(connection: ClientConnection) {
        try {
            while (connection.socket.isConnected && !connection.socket.isClosed) {
                val length = connection.inputStream.readInt()
                val data = ByteArray(length)
                connection.inputStream.readFully(data)

                // 通过 Channel 发送数据
                connection.receiveChannel.send(data)
            }
        } catch (e: Exception) {
            Log.e(TAG, "接收消息失败: ${connection.userId}", e)
        } finally {
            activeClients.remove(connection.userId)
            connection.close()
        }
    }

    /**
     * 发送数据给客户端
     */
    suspend fun sendToClient(userId: String, data: ByteArray): Result<Unit> {
        return runCatching {
            val connection = activeClients[userId]
                ?: throw Exception("客户端未连接: $userId")

            synchronized(connection.outputStream) {
                connection.outputStream.writeInt(data.size)
                connection.outputStream.write(data)
                connection.outputStream.flush()
            }
        }
    }

    /**
     * 停止服务器
     */
    fun stop() {
        activeClients.values.forEach { it.close() }
        activeClients.clear()

        serverSocket?.close()
        serverSocket = null

        scope.cancel()

        Log.d(TAG, "服务器已停止")
    }

    /**
     * 解析 userId（简化版，实际应该解析 JSON）
     */
    private fun parseUserId(data: ByteArray): String? {
        return try {
            // 这里应该解析协议消息获取 userId
            // 暂时返回 null，实际使用时需要实现
            null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 客户端连接
 */
data class ClientConnection(
    val userId: String,
    val socket: Socket,
    val inputStream: DataInputStream,
    val outputStream: DataOutputStream,
    val receiveChannel: kotlinx.coroutines.channels.Channel<ByteArray> =
        kotlinx.coroutines.channels.Channel(kotlinx.coroutines.channels.Channel.UNLIMITED)
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
 * 新连接
 */
data class IncomingConnection(
    val userId: String,
    val connection: ClientConnection
)