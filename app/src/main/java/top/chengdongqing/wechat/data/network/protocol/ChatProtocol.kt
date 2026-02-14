package top.chengdongqing.wechat.data.network.protocol

import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.data.database.entity.MessageType
import top.chengdongqing.wechat.features.chat.domain.model.CallType

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
     * 文件开始
     */
    @Serializable
    data class FileHeader(
        override val messageId: String,
        override val senderId: String,
        val receiverId: String,
        val messageType: MessageType,
        val content: String,
        val fileSize: Long,
        val mediaDuration: Long?,
        val resumeFrom: Long,
        val timestamp: Long
    ) : ChatProtocol()

    /**
     * 文件结束
     */
    @Serializable
    data class FileEnd(
        override val messageId: String,
        override val senderId: String
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

    // 信令消息
    @Serializable
    sealed class Signaling : ChatProtocol() {

        @Serializable
        data class Offer(
            override val messageId: String,
            override val senderId: String,
            val sdp: String
        ) : Signaling()

        @Serializable
        data class Answer(
            override val messageId: String,
            override val senderId: String,
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
        ) : Signaling()

        @Serializable
        data class CallRequest(
            override val messageId: String,
            override val senderId: String,
            val callType: CallType
        ) : Signaling()
    }

    /**
     * 心跳消息
     */
    @Serializable
    data class Heartbeat(
        override val messageId: String = "",
        override val senderId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : ChatProtocol()
}