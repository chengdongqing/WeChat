package top.chengdongqing.wechat.data.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey
    val id: String,                     // 会话ID

    val contactId: String,              // 联系人ID
    val contactName: String,            // 联系人名称
    val contactAvatar: String?,         // 联系人头像

    val lastMessageId: String?,         // 最后一条消息id
    val lastMessage: String?,           // 最后一条消息内容
    val lastMessageType: MessageType?,  // 最后一条消息类型
    val lastMessageTime: Long?,         // 最后一条消息时间

    val unreadCount: Int = 0,           // 未读消息数

    val isSending: Boolean = false,     // 是否发送中
    val isPinned: Boolean = false,      // 是否置顶
    val isMuted: Boolean = false,       // 是否免打扰
    val isHidden: Boolean = false,      // 是否隐藏
    val isSpeakerOn: Boolean = true,    // 是否通过听筒播放

    val draftMessage: String? = null,   // 草稿消息
    val backgroundPath: String? = null, // 聊天背景

    @Embedded
    val audit: EntityAudit = EntityAudit()
)