package top.chengdongqing.wechat.features.chat.domain.model

import top.chengdongqing.wechat.data.database.entity.MessageType

data class ChatSession(
    val sessionId: String,
    val contactId: String,
    val contactName: String,
    val contactAvatar: String?,
    val lastMessage: String?,
    val lastMessageType: MessageType,
    val lastMessageTime: Long,
    val unreadCount: Int,
    val isPinned: Boolean,
    val isMuted: Boolean,
    val draftMessage: String?
)