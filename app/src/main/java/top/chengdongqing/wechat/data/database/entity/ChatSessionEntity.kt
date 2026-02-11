package top.chengdongqing.wechat.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey
    val sessionId: String,              // 会话ID（通常是对方的 userId）

    val contactId: String,              // 联系人ID
    val contactName: String,            // 联系人名称（冗余，方便显示）
    val contactAvatar: String?,         // 联系人头像

    val lastMessage: String?,           // 最后一条消息内容
    val lastMessageType: String?,       // 最后一条消息类型（TEXT, IMAGE, VOICE...）
    val lastMessageTime: Long,          // 最后一条消息时间

    val unreadCount: Int = 0,           // 未读消息数

    val isPinned: Boolean = false,      // 是否置顶
    val isMuted: Boolean = false,       // 是否免打扰

    val draftMessage: String? = null,   // 草稿消息

    val createdAt: Long,                // 创建时间
    val updatedAt: Long                 // 更新时间
)