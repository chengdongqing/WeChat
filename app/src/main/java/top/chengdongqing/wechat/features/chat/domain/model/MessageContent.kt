package top.chengdongqing.wechat.features.chat.domain.model

import top.chengdongqing.wechat.features.call.domain.model.CallStatus
import top.chengdongqing.wechat.features.call.domain.model.CallType

/**
 * 消息内容的类型体系，每种子类对应一种消息形态。
 *
 * 基类提供四个展示控制标志，子类可通过构造参数覆盖默认值：
 * - [showUnreadDot]    是否在会话列表显示未读红点
 * - [showBubbleArrow] 是否显示气泡尖角（纯媒体内容通常不需要）
 * - [showLoading]     是否显示发送中的默认 loading 动画
 * - [isSameBackground] 内容区域是否与气泡背景色一致（卡片类消息需要）
 */
sealed class MessageContent(
    open val showUnreadDot: Boolean = false,
    open val showBubbleArrow: Boolean = true,
    open val showLoading: Boolean = true,
    open val isSameBackground: Boolean = false
) {
    /**
     * 文本消息
     */
    data class Text(val text: String) : MessageContent()

    /**
     * 语音消息
     */
    data class Voice(
        val localPath: String,
        val duration: Long,
        val isPlayed: Boolean = false
    ) : MessageContent(showUnreadDot = !isPlayed)

    /**
     * 表情（贴纸）消息
     */
    data class Sticker(
        val localPath: String
    ) : MessageContent(showBubbleArrow = false)

    /**
     * 图片和视频消息的基类
     */
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

    /**
     * 图片消息
     */
    data class Image(
        override val localPath: String,
        override val filename: String,
        override val mimeType: String,
        override val width: Int,
        override val height: Int,
        override val size: Long
    ) : Media(localPath, filename, mimeType, width, height, size)

    /**
     * 视频消息
     */
    data class Video(
        override val localPath: String,
        override val filename: String,
        override val mimeType: String,
        override val width: Int,
        override val height: Int,
        override val size: Long,
        val duration: Long
    ) : Media(localPath, filename, mimeType, width, height, size)

    /**
     * 通话记录
     */
    data class Call(
        val type: CallType,
        val status: CallStatus,
        val duration: Long? = null
    ) : MessageContent(showUnreadDot = status == CallStatus.Missed)

    /**
     * 位置消息
     */
    data class Location(
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val poiName: String,
        val snapshotPath: String?
    ) : MessageContent(isSameBackground = true)

    /**
     * 文件消息
     */
    data class File(
        val localPath: String,
        val filename: String,
        val mimeType: String,
        val size: Long
    ) : MessageContent(showLoading = false, isSameBackground = true)

    /**
     * 名片消息
     */
    data class ContactCard(
        val userId: String,
        val nickname: String,
        val avatarPath: String
    ) : MessageContent(isSameBackground = true)

    /**
     * 音乐消息
     */
    data class Music(
        val music: MusicTrack
    ) : MessageContent(showBubbleArrow = false)
}