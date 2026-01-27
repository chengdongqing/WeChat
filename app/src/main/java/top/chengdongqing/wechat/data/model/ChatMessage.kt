package top.chengdongqing.wechat.data.model

import top.chengdongqing.wechat.core.utils.format
import kotlin.time.Duration.Companion.milliseconds

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
        val mimeType: String,
        val filename: String,
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
        val duration: Long,
        val mimeType: String,
        val filename: String,
        val width: Int,
        val height: Int,
        val ratio: Float = width.toFloat() / height.toFloat()
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
        val duration: Long? = null
    ) : MessageContent()

    data class Sticker(
        val stickerId: String,
        val localPath: String,
        val description: String? = null
    ) : MessageContent(showBubble = false)

    data class Favorite(
        val title: String,
        val source: String,
        val previewUrl: String? = null
    ) : MessageContent()

    data object Unknown : MessageContent()
}

enum class CallType {
    VOICE, VIDEO;
}

enum class CallStatus(val description: String, val descriptionForMe: String) {
    CANCELLED("对方已取消", "已取消"),
    REJECTED("对方已拒绝", "已拒绝"),
    CONNECTED("已接通", "已接通"),
    MISSED("未应答", "对方无应答");

    companion object {
        fun describeDuration(duration: Long): String {
            return "通话时长 ${duration.milliseconds.format()}"
        }
    }
}