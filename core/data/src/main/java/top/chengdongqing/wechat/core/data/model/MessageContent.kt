package top.chengdongqing.wechat.core.data.model

import kotlinx.serialization.Serializable
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
        open val size: Long,
        open val albumId: String? = null,
        open val albumIndex: Int = 0,
        open val albumSize: Int = 1
    ) : MessageContent(showBubbleArrow = false, showLoading = false) {
        val ratio: Float get() = width.toFloat() / height.toFloat()
    }

    data class Image(
        override val localPath: String,
        override val filename: String,
        override val mimeType: String,
        override val width: Int,
        override val height: Int,
        override val size: Long,
        override val albumId: String? = null,
        override val albumIndex: Int = 0,
        override val albumSize: Int = 1
    ) : Media(localPath, filename, mimeType, width, height, size, albumId, albumIndex, albumSize)

    data class Video(
        override val localPath: String,
        override val filename: String,
        override val mimeType: String,
        override val width: Int,
        override val height: Int,
        override val size: Long,
        val duration: Long,
        override val albumId: String? = null,
        override val albumIndex: Int = 0,
        override val albumSize: Int = 1
    ) : Media(localPath, filename, mimeType, width, height, size, albumId, albumIndex, albumSize)

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

    data class LiveLocation(
        val roomId: String,
        val initiatorId: String
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

    data class Live(
        val liveId: String,
        val title: String,
        val hostName: String,
        val status: String = "live",
        val actorId: String? = null,
        val targetId: String? = null,
        val payload: String? = null
    ) : MessageContent(showBubbleArrow = false, isSameBackground = true)

    /** 多条消息合并后的聊天记录。条目是发送时的快照，不依赖原会话继续存在。 */
    data class ChatHistory(
        val title: String,
        val items: List<ChatHistoryItem>,
        /** 附件归档文件；正文 JSON 与附件分开，归档通过现有媒体分片协议传输。 */
        val archivePath: String? = null
    ) : MessageContent(isSameBackground = true)
}

@Serializable
data class ChatHistoryItem(
    val senderName: String,
    val timestamp: Long,
    val kind: String,
    val text: String,
    val localPath: String? = null,
    val fileSize: Long? = null,
    val duration: Long? = null,
    val mimeType: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val poiName: String? = null,
    val nestedHistory: ChatHistoryPayload? = null,
    val music: MusicTrack? = null
)

@Serializable
data class ChatHistoryPayload(
    val title: String,
    val items: List<ChatHistoryItem>
)
