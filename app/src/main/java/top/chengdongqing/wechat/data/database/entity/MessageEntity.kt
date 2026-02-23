package top.chengdongqing.wechat.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["sessionId", "timestamp"]),  // 按会话和时间查询
        Index(value = ["messageId"], unique = true)  // 消息ID唯一索引
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                   // 本地自增ID

    val messageId: String,              // 全局唯一消息ID
    val sessionId: String,              // 所属会话ID
    val senderId: String,               // 发送者ID
    val receiverId: String,             // 接收者ID

    val contentType: MessageType,       // 消息类型
    val content: String,                // 消息内容（JSON或文本）

    val localPath: String? = null,      // 文件路径
    val fileSize: Long? = null,         // 文件大小
    val mediaDuration: Long? = null,    // 媒体时长

    val timestamp: Long,                // 发送时间戳

    val sentBytes: Long = 0L,           // 已发送字节数（断点位置）
    val sendStatus: SendStatus,         // 发送状态
    val isRead: Boolean = false,        // 是否已读
    val isPlayed: Boolean = false,      // 是否已播放（语音/视频）

    val isFromMe: Boolean,              // 是否是我发送的

    val retryCount: Int = 0,            // 重试次数
    val failReason: SendError? = null,  // 失败原因

    val createdAt: Long,                // 创建时间
    val updatedAt: Long                 // 更新时间
)

@Serializable
enum class MessageType {
    Text,           // 文本
    Voice,          // 语音
    Image,          // 图片
    Video,          // 视频
    File,           // 文件
    Sticker,        // 表情
    Location,       // 位置
    ContactCard,    // 名片
    Favorite,       // 收藏
    VoiceCall,      // 语音通话记录
    VideoCall;       // 视频通话记录

    // 是否需要解析json来获取文件名
    val isFileNameInJson: Boolean
        get() = this == Image || this == Video || this == File

    // 是否为通话消息
    val isCallMessage: Boolean
        get() = this == VideoCall || this == VoiceCall
}

enum class SendStatus {
    Sending,        // 发送中
    Sent,           // 已发送
    Delivered,      // 已送达
    Read,           // 已读
    Failed          // 发送失败
}

enum class SendError(val message: String, val canRetry: Boolean) {
    NetworkTimeout("网络连接超时。", true),
    RecipientOffline("对方不在线。", true),
    NotFriend(
        "对方开启了朋友验证，你还不是他（她）朋友。请先发送朋友验证，对方验证通过后，才能聊天。",
        false
    ),
    Blocked("消息已发出，但被对方拒收了。", false),
    MessageTooLarge("消息内容过大。", false),
    Unknown("未知错误。", true)
}

fun MessageType.toPreviewText(content: String): String = when (this) {
    MessageType.Text -> content
    MessageType.Image -> "[图片]"
    MessageType.Voice -> "[语音]"
    MessageType.Video -> "[视频]"
    MessageType.File -> "[文件]"
    MessageType.Location -> "[位置]"
    MessageType.Favorite -> "[收藏]"
    MessageType.ContactCard -> "[名片]"
    MessageType.Sticker -> "[表情]"
    MessageType.VoiceCall -> "[语音通话]"
    MessageType.VideoCall -> "[视频通话]"
}