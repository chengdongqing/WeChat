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
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.SocketFrame
import top.chengdongqing.wechat.data.network.protocol.SocketProtocol.TYPE_BINARY
import top.chengdongqing.wechat.data.network.protocol.SocketProtocol.TYPE_JSON
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
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
    }

    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _incomingConnections = MutableSharedFlow<IncomingConnection>()
    val incomingConnections: SharedFlow<IncomingConnection> = _incomingConnections.asSharedFlow()

    private val activeClients = mutableMapOf<String, ClientConnection>()

    /**
     * 启动服务器
     */
    suspend fun start(): Int = withContext(Dispatchers.IO) {
        try {
            // 端口设为 0，系统自动分配空闲端口
            val socket = ServerSocket(0).also { serverSocket = it }
            val port = socket.localPort
            Log.d(TAG, "✅ 服务器已在动态端口启动: $port")

            // 在后台开启循环接受连接，不阻塞返回端口的操作
            scope.launch {
                acceptLoop()
            }

            return@withContext port
        } catch (e: Exception) {
            Log.e(TAG, "❌ 服务器启动失败", e)
            -1
        }
    }

    private fun acceptLoop() {
        val socket = serverSocket ?: return
        while (!socket.isClosed) {
            try {
                val clientSocket = socket.accept()
                Log.d(TAG, "✅ 收到新连接: ${clientSocket.inetAddress.hostAddress}")
                handleClient(clientSocket)
            } catch (e: Exception) {
                if (!socket.isClosed) Log.e(TAG, "接受连接异常", e)
            }
        }
    }

    /**
     * 处理客户端连接
     */
    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                val inputStream = DataInputStream(socket.getInputStream())
                val outputStream = DataOutputStream(socket.getOutputStream())

                // 读握手包（第一个 JSON 帧）
                inputStream.readByte()
                val length = inputStream.readInt()
                val data = ByteArray(length)
                inputStream.readFully(data)

                val handshake =
                    json.decodeFromString<ChatProtocol.Heartbeat>(String(data, Charsets.UTF_8))

                val userId = handshake.senderId
                Log.d(TAG, "✅ 握手成功: $userId")
                Log.d("DEBUG", "我是被动接受方 -> 收到 ${userId} 的连接")

                val connection = ClientConnection(
                    userId = userId,
                    socket = socket,
                    inputStream = inputStream,
                    outputStream = outputStream
                )

                activeClients[userId] = connection
                _incomingConnections.emit(IncomingConnection(userId, connection))

                // 持续接收，按类型分发到双通道
                while (!socket.isClosed && socket.isConnected) {
                    try {
                        when (val frameType = inputStream.readByte()) {
                            TYPE_JSON -> {
                                val frameLength = inputStream.readInt()
                                if (frameLength <= 0 || frameLength > 10 * 1024 * 1024) {
                                    Log.e(
                                        TAG,
                                        "❌ 异常 JSON 帧长度: $frameLength，流可能已错位，断开连接"
                                    )
                                    break  // 错位了就断开，continue 会让错误累积
                                }
                                val frameData = ByteArray(frameLength)
                                inputStream.readFully(frameData)
                                connection.frameChannel.send(SocketFrame.JsonFrame(frameData))
                            }

                            TYPE_BINARY -> {
                                // 读 messageId
                                val idLength = inputStream.readByte().toInt()
                                Log.d(TAG, "BINARY idLength: $idLength")

                                if (idLength !in 1..100) {
                                    Log.e(TAG, "❌ 异常 idLength: $idLength，流可能已错位，断开连接")
                                    break
                                }

                                val idBytes = ByteArray(idLength)
                                inputStream.readFully(idBytes)
                                val messageId = String(idBytes, Charsets.UTF_8)
                                Log.d(TAG, "BINARY messageId: $messageId")

                                // 读数据
                                val dataLength = inputStream.readInt()
                                Log.d(TAG, "BINARY dataLength: $dataLength")

                                if (dataLength <= 0 || dataLength > 10 * 1024 * 1024) {
                                    Log.w(TAG, "异常 BINARY 帧长度: $dataLength")
                                    break
                                }
                                val frameData = ByteArray(dataLength)
                                inputStream.readFully(frameData)
                                connection.frameChannel.send(
                                    SocketFrame.BinaryFrame(
                                        messageId,
                                        frameData
                                    )
                                )
                            }

                            else -> {
                                Log.w(TAG, "未知帧类型: $frameType")
                                break
                            }
                        }
                    } catch (_: SocketTimeoutException) {
                        continue
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理客户端失败", e)
            } finally {
                socket.close()
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
}

/**
 * 客户端连接
 */
data class ClientConnection(
    val userId: String,
    val socket: Socket,
    val inputStream: DataInputStream,
    val outputStream: DataOutputStream,
    val frameChannel: Channel<SocketFrame> = Channel(Channel.UNLIMITED)
) {
    fun close() {
        runCatching {
            frameChannel.close()
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