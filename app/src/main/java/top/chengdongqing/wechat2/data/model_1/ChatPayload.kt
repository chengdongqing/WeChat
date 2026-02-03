package top.chengdongqing.wechat2.data.model_1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.core.util.randomUUID

@Serializable
sealed class ChatPayload {

    // 1. 文本消息
    @Serializable
    data class Text(val content: String) : ChatPayload()

    @Serializable
    data class EncryptedText(
        val ciphertext: String, // 密文
        val iv: String          // 初始化向量
    ) : ChatPayload()

    // 2. 表情消息 (微信表情通常是 ID，自定义表情是图片)
    @Serializable
    data class Emoji(val emojiId: String, val category: String) : ChatPayload()

    // 3. 媒体文件 (图片/视频)
    @Serializable
    data class Media(
        val fileId: String,
        val fileName: String,
        val mimeType: String,
        val size: Long,
        val localPath: String? = null,    // 本地路径
        val thumbBase64: String? = null,  // 缩略图（Base64编码，建议控制在10KB以内）
        val duration: Long? = null        // 语音或视频的时长（毫秒）
    ) : ChatPayload()

    @Serializable
    data class EncryptedMedia(
        val mediaType: String,      // "IMAGE", "VIDEO", "AUDIO"
        val ciphertextPath: String, // 加密后的文件路径或 URL
        val iv: String,             // 依旧需要 IV
        val fileName: String        // 文件名
    ) : ChatPayload()

    // 4. 位置消息
    @Serializable
    data class Location(
        val latitude: Double,
        val longitude: Double,
        val address: String
    ) : ChatPayload()

    // 1. 新增：身份信息包
    @Serializable
    data class Identity(val deviceId: String) : ChatPayload()

    // --- 2. 状态信令 (替代之前的 Signal) ---
    @Serializable
    data class CallAction(val action: String) :
        ChatPayload() // "START_VOICE", "START_VIDEO", "HANGUP"

    // --- 3. WebRTC 核心信令 ---
    @Serializable
    @SerialName("webrtc_sdp")
    data class Sdp(
        val sdp: String,
        @SerialName("sdp_type")
        val type: String
    ) : ChatPayload() // type: OFFER, ANSWER

    @Serializable
    @SerialName("webrtc_ice")
    data class Ice(val sdp: String, val sdpMid: String, val sdpMLineIndex: Int) : ChatPayload()
}

val ChatPayload.isSignal: Boolean
    get() = this is ChatPayload.Sdp || this is ChatPayload.Ice || this is ChatPayload.CallAction

/**
 * 消息外壳：包含发送者信息和唯一 ID
 */
@Serializable
data class MessageEnvelope(
    val id: String = randomUUID(),
    val senderId: String,
    val senderName: String,
    val senderIp: String? = null,
    val payload: ChatPayload,
    val timestamp: Long = System.currentTimeMillis()
)