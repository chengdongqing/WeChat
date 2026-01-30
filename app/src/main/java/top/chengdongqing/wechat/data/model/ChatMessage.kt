package top.chengdongqing.wechat.data.model

import android.net.Uri
import top.chengdongqing.wechat.core.utils.format
import top.chengdongqing.wechat.data.call.model.CallType
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
    val showUnreadDot: Boolean = false,
    val showBubbleArrow: Boolean = true,
    val isSameBackground: Boolean = false
) {
    data class Text(val text: String) : MessageContent()

    data class Voice(
        val uri: Uri,
        val duration: Long,
        val isPlayed: Boolean = false
    ) : MessageContent(showUnreadDot = !isPlayed)

    data class Sticker(
        val stickerId: String,
        val localPath: String,
        val description: String? = null
    ) : MessageContent(showBubbleArrow = false)

    abstract class Media(
        open val uri: Uri,
        open val filename: String,
        open val mimeType: String,
        open val width: Int,
        open val height: Int,
    ) : MessageContent(showBubbleArrow = false) {
        val ratio: Float
            get() = width.toFloat() / height.toFloat()
    }

    data class Image(
        override val uri: Uri,
        override val mimeType: String,
        override val filename: String,
        override val width: Int,
        override val height: Int
    ) : Media(uri, filename, mimeType, width, height)

    data class Video(
        override val uri: Uri,
        override val mimeType: String,
        override val filename: String,
        override val width: Int,
        override val height: Int,
        val duration: Long
    ) : Media(uri, filename, mimeType, width, height)

    data class Call(
        val type: CallType,
        val status: CallStatus,
        val duration: Long? = null
    ) : MessageContent(showUnreadDot = status == CallStatus.MISSED)

    data class Location(
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val poiName: String,
        val snapshotUri: Uri?
    ) : MessageContent(isSameBackground = true)

    data class UserCard(
        val userId: String,
        val name: String,
        val avatar: String,
    ) : MessageContent(isSameBackground = true)

    data class File(
        val fileName: String,
        val fileSize: Long,
        val fileType: String,
        val fileUrl: String
    ) : MessageContent(isSameBackground = true)

    data class Favorite(
        val title: String,
        val source: String,
        val previewUrl: String? = null
    ) : MessageContent(isSameBackground = true)

    data object Unknown : MessageContent()
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