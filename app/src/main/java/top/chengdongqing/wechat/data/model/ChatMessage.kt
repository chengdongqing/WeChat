package top.chengdongqing.wechat.data.model

import android.net.Uri

/**
 * 消息数据类
 */
data class ChatMessage(
    val id: String,
    val content: MessageContent,
    val isFromMe: Boolean,
    val timestamp: Long,
    val sendStatus: MessageSendStatus = MessageSendStatus.Success,
    val isSelected: Boolean = false,
    val retryCount: Int = 0  // 重试次数
) {
    /**
     * 是否正在发送中
     */
    val isSending: Boolean
        get() = sendStatus is MessageSendStatus.Sending

    /**
     * 是否发送失败
     */
    val isFailed: Boolean
        get() = sendStatus is MessageSendStatus.Failed

    /**
     * 是否可以重试
     */
    val canRetry: Boolean
        get() = (sendStatus as? MessageSendStatus.Failed)?.canRetry == true

    /**
     * 发送进度（0-1）
     */
    val sendProgress: Float
        get() = (sendStatus as? MessageSendStatus.Sending)?.progress ?: 0f

    /**
     * 错误信息
     */
    val errorMessage: String?
        get() = (sendStatus as? MessageSendStatus.Failed)?.error?.message
}

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
    ) : MessageContent(showUnreadDot = status == CallStatus.Missed)

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

/**
 * 消息发送状态
 */
sealed class MessageSendStatus {
    /** 发送中 */
    data class Sending(val progress: Float = 0f) : MessageSendStatus()

    /** 暂停发送 */
    data class Paused(val progress: Float) : MessageSendStatus()

    /** 发送成功 */
    data object Success : MessageSendStatus()

    /** 发送失败 */
    sealed class Failed : MessageSendStatus() {
        abstract val error: SendError
        abstract val canRetry: Boolean

        /** 网络错误 - 可重试 */
        data class NetworkError(
            override val error: SendError = SendError.NETWORK_TIMEOUT
        ) : Failed() {
            override val canRetry: Boolean = true
        }

        /** 对方不在线 - 可重试 */
        data class RecipientOffline(
            override val error: SendError = SendError.RECIPIENT_OFFLINE
        ) : Failed() {
            override val canRetry: Boolean = true
        }

        /** 不是好友 - 不可重试 */
        data class NotFriend(
            override val error: SendError = SendError.NOT_FRIEND
        ) : Failed() {
            override val canRetry: Boolean = false
        }

        /** 被对方拉黑 - 不可重试 */
        data class Blocked(
            override val error: SendError = SendError.BLOCKED
        ) : Failed() {
            override val canRetry: Boolean = false
        }

        /** 消息过大 - 不可重试 */
        data class MessageTooLarge(
            override val error: SendError = SendError.MESSAGE_TOO_LARGE
        ) : Failed() {
            override val canRetry: Boolean = false
        }
    }
}

/**
 * 错误类型枚举
 */
enum class SendError(val message: String) {
    NETWORK_TIMEOUT("网络连接超时。"),
    RECIPIENT_OFFLINE("对方不在线。"),
    NOT_FRIEND("对方已不是你的好友。"),
    BLOCKED("消息已发出，但被对方拒收了。"),
    MESSAGE_TOO_LARGE("消息内容过大。"),
    UNKNOWN("未知错误。")
}