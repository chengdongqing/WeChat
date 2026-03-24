package top.chengdongqing.wechat.core.data.model

import top.chengdongqing.wechat.core.common.call.CallStatus
import top.chengdongqing.wechat.core.model.CallType

sealed class MessageContent(
    open val showUnreadDot: Boolean = false,
    open val showBubbleArrow: Boolean = true,
    open val showLoading: Boolean = true,
    open val isSameBackground: Boolean = false
) {
    data class Text(val text: String) : MessageContent()

    data class Voice(
        val localPath: String,
        val duration: Long,
        val isPlayed: Boolean = false
    ) : MessageContent(showUnreadDot = !isPlayed)

    data class Sticker(val localPath: String) : MessageContent(showBubbleArrow = false)

    abstract class Media(
        open val localPath: String,
        open val filename: String,
        open val mimeType: String,
        open val width: Int,
        open val height: Int,
        open val size: Long
    ) : MessageContent(showBubbleArrow = false, showLoading = false) {
        val ratio: Float get() = width.toFloat() / height.toFloat()
    }

    data class Image(
        override val localPath: String,
        override val filename: String,
        override val mimeType: String,
        override val width: Int,
        override val height: Int,
        override val size: Long
    ) : Media(localPath, filename, mimeType, width, height, size)

    data class Video(
        override val localPath: String,
        override val filename: String,
        override val mimeType: String,
        override val width: Int,
        override val height: Int,
        override val size: Long,
        val duration: Long
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
        val filename: String,
        val mimeType: String,
        val size: Long
    ) : MessageContent(showLoading = false, isSameBackground = true)

    data class ContactCard(
        val userId: String,
        val nickname: String,
        val avatarPath: String
    ) : MessageContent(isSameBackground = true)

    data class Music(val music: MusicTrack) : MessageContent(showBubbleArrow = false)
}
