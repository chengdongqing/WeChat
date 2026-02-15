package top.chengdongqing.wechat.features.chat.domain.model

import top.chengdongqing.wechat.data.database.entity.MessageType

data class ChatSession(
    val sessionId: String,
    val contactId: String,
    val contactName: String,
    val contactAvatar: String? = null,
    val lastMessage: String? = null,
    val lastMessageType: MessageType? = null,
    val lastMessageTime: Long? = null,
    val draftMessage: String? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isHidden: Boolean = false
)