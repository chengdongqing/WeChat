package top.chengdongqing.wechat.data.network.protocol

import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.data.database.entity.MessageType

/**
 * 聊天消息协议
 */
@Serializable
sealed class ChatProtocol {

    /**
     * 文本消息
     */
    @Serializable
    data class TextMessage(
        val messageId: String,
        val senderId: String,
        val receiverId: String,
        val content: String,
        val timestamp: Long
    ) : ChatProtocol()

    /**
     * 媒体消息（图片、语音、视频、文件）
     */
    @Serializable
    data class MediaMessage(
        val messageId: String,
        val senderId: String,
        val receiverId: String,
        val messageType: MessageType,
        val fileName: String,
        val fileSize: Long,
        val mediaWidth: Int? = null,
        val mediaHeight: Int? = null,
        val mediaDuration: Int? = null,
        val timestamp: Long
    ) : ChatProtocol()

    /**
     * 消息确认（已送达）
     */
    @Serializable
    data class MessageAck(
        val messageId: String,
        val receiverId: String,
        val timestamp: Long
    ) : ChatProtocol()

    /**
     * 消息已读回执
     */
    @Serializable
    data class MessageRead(
        val messageId: String,
        val receiverId: String,
        val timestamp: Long
    ) : ChatProtocol()

    /**
     * 在线状态
     */
    @Serializable
    data class OnlineStatus(
        val userId: String,
        val isOnline: Boolean,
        val timestamp: Long
    ) : ChatProtocol()

    /**
     * 心跳包
     */
    @Serializable
    data class Heartbeat(
        val userId: String,
        val timestamp: Long
    ) : ChatProtocol()
}