package top.chengdongqing.wechat.data.network.protocol

import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.data.database.entity.MessageType
import top.chengdongqing.wechat.features.call.domain.model.CallType
import top.chengdongqing.wechat.features.call.domain.model.HangupReason

/**
 * 聊天消息协议
 */
@Serializable
sealed class ChatProtocol {

    abstract val messageId: String
    abstract val senderId: String

    /**
     * 文本消息
     */
    @Serializable
    data class TextMessage(
        override val messageId: String,
        override val senderId: String,
        val receiverId: String,
        val content: String,
        val timestamp: Long
    ) : ChatProtocol()

    /**
     * 媒体消息（图片、语音、视频、文件等）
     */
    @Serializable
    data class MediaMessage(
        override val messageId: String,
        override val senderId: String,
        val receiverId: String,
        val messageType: MessageType,
        val content: String,
        val fileSize: Long,
        val checksum: String? = null,
        val mediaDuration: Long? = null,
        val timestamp: Long
    ) : ChatProtocol()

    /**
     * 消息确认（已送达）
     */
    @Serializable
    data class MessageAck(
        override val messageId: String,
        override val senderId: String,
        val timestamp: Long
    ) : ChatProtocol()

    /**
     * 消息已读回执
     */
    @Serializable
    data class MessageRead(
        override val messageId: String,
        override val senderId: String,
        val timestamp: Long
    ) : ChatProtocol()

    /**
     * WebRTC 信令消息
     */
    @Serializable
    sealed class Signaling : ChatProtocol() {

        @Serializable
        data class Offer(
            override val messageId: String,
            override val senderId: String,
            val callType: CallType,
            val sdp: String
        ) : Signaling()

        @Serializable
        data class Answer(
            override val messageId: String,
            override val senderId: String,
            val callType: CallType,
            val sdp: String
        ) : Signaling()

        @Serializable
        data class IceCandidate(
            override val messageId: String,
            override val senderId: String,
            val candidate: String,
            val sdpMid: String?,
            val sdpMLineIndex: Int
        ) : Signaling()

        @Serializable
        data class Hangup(
            override val messageId: String,
            override val senderId: String,
            val reason: HangupReason
        ) : Signaling()

        @Serializable
        data class Busy(
            override val messageId: String,
            override val senderId: String,
        ) : Signaling()
    }

    /**
     * 握手包
     */
    @Serializable
    data class Handshake(
        override val messageId: String = "",
        override val senderId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : ChatProtocol()
}