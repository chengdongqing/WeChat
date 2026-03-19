package top.chengdongqing.wechat.data.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import top.chengdongqing.wechat.data.model.MessageType
import top.chengdongqing.wechat.data.model.SendError
import top.chengdongqing.wechat.data.model.SendStatus

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["sessionId", "timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,                     // 消息ID
    val sessionId: String,              // 所属会话ID
    val senderId: String,               // 发送者ID
    val receiverId: String,             // 接收者ID

    val contentType: MessageType,       // 消息类型
    val content: String,                // 消息内容

    val localPath: String? = null,      // 文件路径
    val fileSize: Long? = null,         // 文件大小
    val mediaDuration: Long? = null,    // 媒体时长

    val timestamp: Long,                // 发送时间戳
    val sentBytes: Long = 0L,           // 已发送字节数（断点位置）
    val sendStatus: SendStatus,         // 发送状态
    val isRead: Boolean = false,        // 是否已读
    val isPlayed: Boolean = false,      // 是否已播放（语音/视频）
    val isRecalled: Boolean = false,    // 是否已撤回
    val isFromMe: Boolean,              // 是否我发送的
    val failReason: SendError? = null,  // 发送失败原因

    @Embedded
    val audit: EntityAudit = EntityAudit()
)

val MessageEntity.peerId: String
    get() = if (isFromMe) receiverId else senderId