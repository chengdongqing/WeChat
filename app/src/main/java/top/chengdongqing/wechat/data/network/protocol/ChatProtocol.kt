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

    /** 文本消息 */
    @Serializable
    data class TextMessage(
        override val messageId: String,
        override val senderId: String,
        val receiverId: String,
        val content: String,
        val timestamp: Long
    ) : ChatProtocol()

    /** 通话结束记录，通话完成后由主动挂断方发送 */
    data class CallMessage(
        override val messageId: String,
        override val senderId: String,
        val receiverId: String,
        val status: String,
        val duration: Long,
        val callType: CallType,
        val timestamp: Long
    ) : ChatProtocol()

    /**
     * 媒体消息（图片、语音、视频、文件等）
     *
     * 通过 FILE_META + FILE_CHUNK 分片传输，此协议包作为元数据随 FILE_META 发送。
     * checksum 为文件 MD5，接收端校验通过后再入库。
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

    /** 送达回执，接收端收到消息后立即回复 */
    @Serializable
    data class MessageAck(
        override val messageId: String,
        override val senderId: String,
        val timestamp: Long
    ) : ChatProtocol()

    /** 已读回执，用户打开会话后对未读消息逐条发送 */
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

        /** 发起通话，携带 SDP（编解码、传输协议、加密协议等能力描述） */
        @Serializable
        data class Offer(
            override val messageId: String,
            override val senderId: String,
            val callType: CallType,
            val sdp: String
        ) : Signaling()

        /** 接受通话，携带己方 SDP */
        @Serializable
        data class Answer(
            override val messageId: String,
            override val senderId: String,
            val callType: CallType,
            val sdp: String
        ) : Signaling()

        /**
         * ICE 候选路径
         *
         * candidate：候选地址，包含 IP、端口、协议（TCP/UDP）及优先级
         * sdpMid：所属媒体流标识（audio / video）
         * sdpMLineIndex：对应 SDP 中 m= 行的索引（通常 0=音频，1=视频）
         */
        @Serializable
        data class IceCandidate(
            override val messageId: String,
            override val senderId: String,
            val candidate: String,
            val sdpMid: String?,
            val sdpMLineIndex: Int
        ) : Signaling()

        /** 挂断，携带原因（主动挂断 / 超时 / 拒绝等） */
        @Serializable
        data class Hangup(
            override val messageId: String,
            override val senderId: String,
            val reason: HangupReason
        ) : Signaling()

        /** 忙线，对方正在通话中时回复 */
        @Serializable
        data class Busy(
            override val messageId: String,
            override val senderId: String,
        ) : Signaling()

        /** 媒体状态变更（摄像头开关、麦克风静音、扬声器切换） */
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
     *
     * TCP 连接建立后的第一个包，携带 senderId 供对端识别身份。
     * 同时承载 E2E 密钥交换：
     *   e2ePublicKey    非空时表示主动发起 E2E 握手
     *   e2ePublicKeyAck 非空时表示响应 E2E 握手
     */
    @Serializable
    data class Handshake(
        override val messageId: String = "",
        override val senderId: String,
        val timestamp: Long = System.currentTimeMillis(),
        val e2ePublicKey: String? = null,
        val e2ePublicKeyAck: String? = null
    ) : ChatProtocol()
}