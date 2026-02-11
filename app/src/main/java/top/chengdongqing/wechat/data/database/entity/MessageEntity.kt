package top.chengdongqing.wechat.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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

    val localPath: String? = null,      // 本地媒体文件路径
    val remotePath: String? = null,     // 远程媒体文件路径（如果有服务器）

    val mediaSize: Long? = null,        // 媒体文件大小
    val mediaDuration: Int? = null,     // 媒体时长（秒）
    val mediaWidth: Int? = null,        // 图片/视频宽度
    val mediaHeight: Int? = null,       // 图片/视频高度

    val timestamp: Long,                // 发送时间戳

    val sendStatus: SendStatus,         // 发送状态
    val isRead: Boolean = false,        // 是否已读
    val isPlayed: Boolean = false,      // 是否已播放（语音/视频）

    val isFromMe: Boolean,              // 是否是我发送的

    val retryCount: Int = 0,            // 重试次数

    val createdAt: Long,                // 创建时间
    val updatedAt: Long                 // 更新时间
)

enum class MessageType {
    TEXT,           // 文本
    IMAGE,          // 图片
    VOICE,          // 语音
    VIDEO,          // 视频
    FILE,           // 文件
    EMOJI,          // 表情
    LOCATION,       // 位置
    CONTACT_CARD,   // 名片
    VOICE_CALL,     // 语音通话记录
    VIDEO_CALL      // 视频通话记录
}

enum class SendStatus {
    SENDING,        // 发送中
    SENT,           // 已发送
    DELIVERED,      // 已送达
    READ,           // 已读
    FAILED          // 发送失败
}