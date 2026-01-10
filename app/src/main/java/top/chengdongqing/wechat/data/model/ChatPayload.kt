package top.chengdongqing.wechat.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed class ChatPayload {

    // 1. 文本消息
    @Serializable
    data class Text(val content: String) : ChatPayload()

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

    // 4. 位置消息
    @Serializable
    data class Location(
        val latitude: Double,
        val longitude: Double,
        val address: String
    ) : ChatPayload()

    // 5. 实时信令 (WebRTC 握手、输入中状态、实时位置开启)
    @Serializable
    data class Signal(
        val action: String, // "START_CALL", "OFFER", "ANSWER", "TYPING"
        val data: String? = null
    ) : ChatPayload()

    // 1. 新增：身份信息包
    @Serializable
    data class Identity(val deviceId: String) : ChatPayload()
}

/**
 * 消息外壳：包含发送者信息和唯一 ID
 */
@Serializable
data class MessageEnvelope(
    val id: String, // 建议用 UUID
    val senderId: String, // 设备的唯一标识 (MAC 地址或自定义 ID)
    val senderName: String,
    val payload: ChatPayload,
    val timestamp: Long = System.currentTimeMillis()
)