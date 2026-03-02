package top.chengdongqing.wechat.features.chat.domain.model

import top.chengdongqing.wechat.core.util.isWithinSeconds
import top.chengdongqing.wechat.data.database.entity.SendError
import top.chengdongqing.wechat.features.call.domain.model.CallStatus
import top.chengdongqing.wechat.features.call.domain.model.CallType

/**
 * 消息数据类
 */
data class ChatMessage(
    val id: String,
    val sessionId: String,
    val senderId: String,
    val content: MessageContent,
    val isRecalled: Boolean,
    val isFromMe: Boolean,
    val timestamp: Long,
    val sendStatus: MessageSendStatus = MessageSendStatus.Success
) {
    /**
     * 是否正在发送中
     */
    val isSending: Boolean
        get() = sendStatus is MessageSendStatus.Sending

    /**
     * 是否已发送完成
     *
     * 在发送完成15秒之后还没有收到送达回执将给出提示
     */
    val isSent: Boolean
        get() = sendStatus is MessageSendStatus.Sent && !timestamp.isWithinSeconds(15)

    /**
     * 是否发送失败
     */
    val isFailed: Boolean
        get() = sendStatus is MessageSendStatus.Failed

    /**
     * 发送进度（0-1）
     */
    val sendProgress: Float
        get() = (sendStatus as? MessageSendStatus.Sending)?.progress ?: 0f

    /**
     * 错误信息
     */
    val error: SendError?
        get() = (sendStatus as? MessageSendStatus.Failed)?.error
}

/**
 * 消息内容结构
 */
sealed class MessageContent(
    val showUnreadDot: Boolean = false,
    val showBubbleArrow: Boolean = true,
    val showLoading: Boolean = true,
    val isSameBackground: Boolean = false
) {
    data class Text(val text: String) : MessageContent()

    data class Voice(
        val localPath: String,
        val duration: Long,
        val isPlayed: Boolean = false
    ) : MessageContent(showUnreadDot = !isPlayed)

    data class Sticker(
        val localPath: String,
        val description: String? = null
    ) : MessageContent(showBubbleArrow = false)

    abstract class Media(
        open val localPath: String,
        open val filename: String,
        open val mimeType: String,
        open val width: Int,
        open val height: Int,
        open val size: Long
    ) : MessageContent(showBubbleArrow = false, showLoading = false) {
        val ratio: Float
            get() = width.toFloat() / height.toFloat()
    }

    data class Image(
        override val localPath: String,
        override val mimeType: String,
        override val filename: String,
        override val width: Int,
        override val height: Int,
        override val size: Long
    ) : Media(localPath, filename, mimeType, width, height, size)

    data class Video(
        override val localPath: String,
        override val mimeType: String,
        override val filename: String,
        override val width: Int,
        override val height: Int,
        override val size: Long,
        val duration: Long,
    ) : Media(localPath, filename, mimeType, width, height, size)

    data class Call(
        val type: CallType,
        val status: CallStatus,
        val duration: Long? = null
    ) : MessageContent(showUnreadDot = status == CallStatus.Missed)

    data class Location(
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val poiName: String,
        val snapshotPath: String?
    ) : MessageContent(isSameBackground = true)

    data class File(
        val localPath: String,
        val mimeType: String,
        val filename: String,
        val size: Long
    ) : MessageContent(showLoading = false, isSameBackground = true)

    data class ContactCard(
        val userId: String,
        val name: String,
        val avatar: String,
    ) : MessageContent(isSameBackground = true)

    data class Favorite(
        val title: String,
        val source: String,
        val previewPath: String? = null
    ) : MessageContent(isSameBackground = true)
}

/**
 * 消息发送状态
 */
sealed class MessageSendStatus {
    /** 发送中 */
    data class Sending(val progress: Float = 0f) : MessageSendStatus()

    /** 暂停发送 */
    data class Paused(val progress: Float) : MessageSendStatus()

    /** 已发送，未收到回执 */
    data object Sent : MessageSendStatus()

    /** 发送成功 */
    data object Success : MessageSendStatus()

    /** 发送失败 */
    data class Failed(val error: SendError) : MessageSendStatus()
}

fun MessageContent.toPreviewText(): String = when (this) {
    is MessageContent.Text -> text
    is MessageContent.Image -> "[图片]"
    is MessageContent.Voice -> "[语音]"
    is MessageContent.Video -> "[视频]"
    is MessageContent.File -> "[文件]"
    is MessageContent.Location -> "[位置]"
    is MessageContent.Favorite -> "[收藏]"
    is MessageContent.ContactCard -> "[名片]"
    is MessageContent.Sticker -> "[表情]"
    is MessageContent.Call -> "[${type.label}]"
    else -> ""
}