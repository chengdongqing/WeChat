package top.chengdongqing.wechat.features.call.manager

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketType
import top.chengdongqing.wechat.data.network.socket.SocketManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalingManager @Inject constructor(
    private val socketManager: SocketManager,
    private val json: Json
) {
    private companion object {
        const val TAG = "SignalingManager"
    }

    private val _incomingSignaling =
        MutableSharedFlow<ChatProtocol.Signaling>(extraBufferCapacity = 16)
    val incomingSignaling = _incomingSignaling.asSharedFlow()

    /** 发送信令（CallManager 调用） */
    suspend fun send(targetUserId: String, message: ChatProtocol.Signaling) {
        val jsonBytes = json.encodeToString<ChatProtocol.Signaling>(message)
            .toByteArray(Charsets.UTF_8)

        socketManager.send(targetUserId, Packet(PacketType.SIGNALING, jsonBytes))
            .onSuccess { Log.d(TAG, "→ ${message::class.simpleName} to $targetUserId") }
            .onFailure { Log.e(TAG, "发送失败: ${message::class.simpleName}", it) }
    }

    /**
     * 收到信令（由 MessageDispatcher 调用）
     */
    suspend fun onSignalingReceived(protocol: ChatProtocol.Signaling) {
        try {
            Log.d(TAG, "← ${protocol::class.simpleName} from ${protocol.senderId}")
            _incomingSignaling.emit(protocol)
        } catch (e: Exception) {
            Log.e(TAG, "信令解析失败", e)
        }
    }
}