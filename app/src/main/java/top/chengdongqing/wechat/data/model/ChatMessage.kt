package top.chengdongqing.wechat.data.model

/**
 * 消息基础信息
 */
data class ChatMessage(
    val id: String,
    val content: MessageContent,
    val isFromMe: Boolean,
    val timestamp: Long,
    val isSelected: Boolean = false
)

/**
 * 消息内容结构
 */
sealed class MessageContent(
    val showBubble: Boolean = true,
    val clickable: Boolean = true
) {
    data class Text(val text: String) : MessageContent(clickable = false)

    data class Image(
        val url: String,
        val width: Int,
        val height: Int,
        val ratio: Float = width.toFloat() / height.toFloat()
    ) : MessageContent(showBubble = false)

    data class Voice(
        val url: String,
        val duration: Int,
        val isPlayed: Boolean = false
    ) : MessageContent()

    data class Video(
        val videoUrl: String,
        val coverUrl: String,
        val duration: Long
    ) : MessageContent(showBubble = false)

    data class Location(
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val poiName: String,
        val snapshotUrl: String
    ) : MessageContent()

    data class UserCard(
        val userId: String,
        val name: String,
        val avatar: String,
        val weChatId: String
    ) : MessageContent()

    data class File(
        val fileName: String,
        val fileSize: Long,
        val fileType: String,
        val fileUrl: String
    ) : MessageContent()

    data class Call(
        val type: CallType,
        val status: CallStatus,
        val durationText: String? = null
    ) : MessageContent()

    data class Sticker(
        val stickerId: String,
        val url: String,
        val isAnimated: Boolean = false
    ) : MessageContent(showBubble = false)

    data class Favorite(
        val title: String,
        val source: String,
        val previewUrl: String? = null
    ) : MessageContent()

    data object Unknown : MessageContent()
}

enum class CallType { VOICE, VIDEO }
enum class CallStatus { CANCELLED, REJECTED, CONNECTED, MISSED }