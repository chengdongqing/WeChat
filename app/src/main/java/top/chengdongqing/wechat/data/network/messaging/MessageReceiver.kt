package top.chengdongqing.wechat.data.network.messaging

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
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
        scope.launch {
            for (data in connection.receiveChannel) {
                handleData(data)
            }
            Log.d(TAG, "连接已关闭: ${connection.userId}")
        }
    }

    /**
     * 监听服务端连接（对方连进来的）
     */
    fun startListening(connection: ClientConnection) {
        scope.launch {
            for (data in connection.receiveChannel) {
                handleData(data)
            }
            Log.d(TAG, "客户端已断开: ${connection.userId}")
        }
    }

    /**
     * 解析数据并交给 Dispatcher
     */
    private suspend fun handleData(data: ByteArray) {
        try {
            val jsonString = String(data, Charsets.UTF_8)

            val protocol = json.decodeFromString<ChatProtocol>(jsonString)
            dispatcher.dispatch(protocol)
        } catch (e: Exception) {
            Log.e(TAG, "解析消息失败", e)
        }
    }
}