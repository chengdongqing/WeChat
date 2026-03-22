package top.chengdongqing.wechat.features.call.manager

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.network.connection.ChatTransportManager
import top.chengdongqing.wechat.data.network.model.ChatProtocol
import top.chengdongqing.wechat.data.network.model.Packet
import top.chengdongqing.wechat.data.network.model.PacketType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebRTC 信令管理器
 */
@Singleton
class SignalingManager @Inject constructor(
    private val transport: ChatTransportManager,
    private val json: Json
) {
    private companion object {
        const val TAG = "SignalingManager"
    }

    private val _incomingSignaling =
        MutableSharedFlow<ChatProtocol.Signaling>(extraBufferCapacity = 16)

    /** 入站信令流，[CallManager] 订阅后处理 Offer/Answer/ICE/Hangup 等 */
    val incomingSignaling = _incomingSignaling.asSharedFlow()

    /**
     * 发送信令
     *
     * 由 [CallManager] 调用，序列化后通过 TCP 发给对端。
     */
    suspend fun send(targetUserId: String, message: ChatProtocol.Signaling) {
        val body = json.encodeToString<ChatProtocol.Signaling>(message).toByteArray(Charsets.UTF_8)
        transport.send(
            userId = targetUserId,
            packet = Packet(PacketType.SIGNALING, body)
        ).onFailure {
            Log.w(TAG, "发送失败: ${message::class.simpleName}")
            throw it
        }
    }

    /**
     * 处理收到的信令
     *
     * 由 [top.chengdongqing.wechat.data.network.messaging.MessageDispatcher] 调用，
     * 推入流后由 [CallManager] 消费。
     */
    suspend fun onSignalingReceived(protocol: ChatProtocol.Signaling) {
        runCatching { _incomingSignaling.emit(protocol) }
            .onFailure { Log.e(TAG, "信令推送失败", it) }
    }
}