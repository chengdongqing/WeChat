package top.chengdongqing.wechat.features.chat.domain.model

import androidx.compose.runtime.Immutable
import top.chengdongqing.wechat.core.util.isWithinSeconds
import top.chengdongqing.wechat.data.model.SendError
import top.chengdongqing.wechat.features.call.domain.model.CallStatus
import top.chengdongqing.wechat.features.call.domain.model.CallType

/**
 * 聊天消息的完整描述，包含消息元数据和当前发送状态。
 */
@Immutable
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
    val isSending: Boolean
        get() = sendStatus is MessageSendStatus.Sending

    /**
     * 消息已发出但尚未收到送达回执。
     *
     * 发出后 15 秒内视为正常网络延迟，不做任何提示；
     * 超过 15 秒仍未收到回执则将此标志置为 true，UI 层可据此展示"未送达"提示。
     * 一旦收到回执，[sendStatus] 会从 [MessageSendStatus.Sent] 跳转为
     * [MessageSendStatus.Success]，此属性随之变为 false。
     */
    val isSent: Boolean
        get() = sendStatus is MessageSendStatus.Sent && !timestamp.isWithinSeconds(15)

    val isFailed: Boolean
        get() = sendStatus is MessageSendStatus.Failed

    /**
     * 当前上传/发送进度，范围 0.0 ~ 1.0。
     */
    val sendProgress: Float
        get() = (sendStatus as? MessageSendStatus.Sending)?.progress ?: 0f

    /**
     * 发送失败的具体原因，仅 [MessageSendStatus.Failed] 状态下非空。
     * UI 层可用于展示错误提示文案或决定是否允许重试。
     */
    val error: SendError?
        get() = (sendStatus as? MessageSendStatus.Failed)?.error
}

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
    /** 普通文本消息 */
    data class Text(val text: String) : MessageContent()

    /**
     * 语音消息。
     *
     * [duration] 单位为毫秒
     * [isPlayed] 表示当前用户是否已收听过，未播放时触发 [showUnreadDot] 展示红点
     */
    data class Voice(
        val localPath: String,
        val duration: Long,
        val isPlayed: Boolean = false
    ) : MessageContent(showUnreadDot = !isPlayed)

    /**
     * 表情（贴纸）消息。
     *
     * 贴纸本身已经是视觉焦点，不需要气泡尖角修饰，故关闭 [showBubbleArrow]。
     * [description] 用于无障碍（TalkBack）场景的语义描述，可为空。
     */
    data class Sticker(
        val localPath: String,
        val description: String? = null
    ) : MessageContent(showBubbleArrow = false)

    /**
     * 图片和视频消息的公共抽象基类。
     *
     * 媒体内容撑满气泡，不需要尖角（[showBubbleArrow] = false），
     * 也不需要默认 loading （[showLoading] = false），加载状态由缩略图内部处理。
     *
     * [ratio] 用于在图片加载完成前为 item 预留精确高度，避免列表滚动时的布局抖动。
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
     * 图片消息。
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
     * 视频消息。
     * [duration] 单位为毫秒
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
     * 通话记录消息。
     *
     * [status] 为 [CallStatus.Missed] 时触发未读红点，提示用户有未接来电。
     * [duration] 仅在通话正常结束（[CallStatus.Finished]）后非空，单位为秒。
     */
    data class Call(
        val type: CallType,
        val status: CallStatus,
        val duration: Long? = null
    ) : MessageContent(showUnreadDot = status == CallStatus.Missed)

    /**
     * 位置消息。
     *
     * [snapshotPath] 为地图截图的本地缓存路径
     * [isSameBackground] = true 使地图截图与气泡背景无缝融合。
     */
    data class Location(
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val poiName: String,
        val snapshotPath: String?
    ) : MessageContent(isSameBackground = true)

    /**
     * 文件消息。
     *
     * [localPath] 为下载完成后的本地路径，未下载时为空字符串，
     * UI 层通过是否为空决定展示"下载"还是"打开"按钮。
     * [isSameBackground] = true 使文件卡片背景与气泡色一致，视觉上形成卡片效果。
     */
    data class File(
        val localPath: String,
        val filename: String,
        val mimeType: String,
        val size: Long
    ) : MessageContent(showLoading = false, isSameBackground = true)

    /**
     * 名片消息。
     */
    data class ContactCard(
        val userId: String,
        val name: String,
        val avatar: String
    ) : MessageContent(isSameBackground = true)

    /**
     * 收藏消息。
     */
    data class Favorite(
        val title: String,
        val source: String,
        val previewPath: String? = null
    ) : MessageContent(isSameBackground = true)
}

/**
 * 消息发送状态的完整生命周期。
 *
 * 正常流转：[Sending] → [Sent] → [Success]
 * 异常流转：[Sending] → [Failed]
 * 暂停续传：[Sending] → [Paused] → [Sending]（断点续传场景）
 *
 * 注意：[Sent] 与 [Success] 是两个不同阶段——
 * [Sent] 表示消息已发送，但尚未确认对方设备收到；
 * [Success] 表示收到了送达回执，确认消息已成功投递。
 */
sealed class MessageSendStatus {
    /** 发送中，[progress] 表示当前上传进度（0.0 ~ 1.0），纯文本消息通常直接跳过此状态。 */
    data class Sending(val progress: Float = 0f) : MessageSendStatus()

    /** 已暂停（断点续传），[progress] 保存暂停时的进度，恢复时从此处继续。 */
    data class Paused(val progress: Float) : MessageSendStatus()

    /** 已发出，等待送达回执。超过 15 秒未收到回执时 UI 层给出提示，见 [ChatMessage.isSent]。 */
    data object Sent : MessageSendStatus()

    /** 已送达，收到回执确认对方设备已接收。 */
    data object Success : MessageSendStatus()

    /** 发送失败，[error] 携带失败原因，UI 层据此决定提示文案和重试策略。 */
    data class Failed(val error: SendError) : MessageSendStatus()
}

/**
 * 将消息内容转换为会话列表的单行预览文本。
 */
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
    else -> "消息"
}