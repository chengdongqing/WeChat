package top.chengdongqing.wechat.data.network.messaging

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.SocketFrame
import top.chengdongqing.wechat.data.network.socket.ClientConnection
import top.chengdongqing.wechat.data.network.socket.SocketConnection
import top.chengdongqing.wechat.data.network.socket.SocketServer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息接收器
 */
@Singleton
class MessageReceiver @Inject constructor(
    private val socketServer: SocketServer,
    private val dispatcher: MessageDispatcher,
    private val json: Json
) {
    private companion object {
        const val TAG = "MessageReceiver"
    }

    // 透传 Flow
    val incomingMessageFlow = dispatcher.incomingMessageFlow
    val signalingFlow = dispatcher.signalingFlow

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 启动服务器端连接监听
     */
    fun start() {
        scope.launch {
            socketServer.incomingConnections.collect { incoming ->
                startListening(incoming.connection)
            }
        }
        Log.d(TAG, "✅ 消息接收器已启动")
    }

    /**
     * 监听客户端连接（主动连出去的）
     */
    fun startListening(connection: SocketConnection) {
        listenChannels(connection.userId, connection.frameChannel)
    }

    /**
     * 监听服务端连接（对方连进来的）
     */
    fun startListening(connection: ClientConnection) {
        listenChannels(connection.userId, connection.frameChannel)
    }

    private fun listenChannels(
        userId: String,
        frameChannel: Channel<SocketFrame>,
    ) {
        Log.d(TAG, "启动监听: $userId")

        scope.launch {
            for (frame in frameChannel) {
                when (frame) {
                    is SocketFrame.JsonFrame -> {
                        handleJsonData(frame.data)
                    }

                    is SocketFrame.BinaryFrame -> {
                        handleBinaryData(frame)
                    }
                }
            }
            Log.d(TAG, "二进制通道关闭: $userId")
        }
    }

    private suspend fun handleJsonData(data: ByteArray) {
        try {
            val jsonString = String(data, Charsets.UTF_8)
            val protocol = json.decodeFromString<ChatProtocol>(jsonString)
            dispatcher.dispatch(protocol)
        } catch (e: Exception) {
            Log.e(TAG, "JSON 解析失败: ${e.message}")
        }
    }

    private suspend fun handleBinaryData(frame: SocketFrame.BinaryFrame) {
        try {
            dispatcher.dispatchBinary(frame)
        } catch (e: Exception) {
            Log.e(TAG, "二进制处理失败: ${frame.messageId} - ${e.message}")
        }
    }
}