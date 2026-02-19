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

        /**
         * 发起
         */
        @Serializable
        data class Offer(
            override val messageId: String,
            override val senderId: String,
            val callType: CallType,
            val sdp: String // 支持的编解码方式、传输协议等
        ) : Signaling()

        /**
         * 回应
         */
        @Serializable
        data class Answer(
            override val messageId: String,
            override val senderId: String,
            val callType: CallType,
            val sdp: String
        ) : Signaling()

        /**
         * 网络候选路径
         */
        @Serializable
        data class IceCandidate(
            override val messageId: String,
            override val senderId: String,
            val candidate: String, // 候选地址描述：包含IP地址、端口号、协议（TCP/UDP）以及优先级
            val sdpMid: String?, // 标识该地址属于哪个媒体流（video 或 audio）
            val sdpMLineIndex: Int // 对应SDP中 m= 行的索引位置（通常0代表音频，1代表视频）
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

        @Serializable
        data class MediaState(
            override val messageId: String,
            override val senderId: String,
            val isVideoOn: Boolean = true,
            val isMicOn: Boolean = true,
            val isSpeakerOn: Boolean = true
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