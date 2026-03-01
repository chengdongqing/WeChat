package top.chengdongqing.wechat.features.chat.domain.model

import top.chengdongqing.wechat.data.database.entity.MessageType

data class ChatSession(
    val id: String,
    val contactId: String,
    val contactName: String,
    val contactAvatar: String? = null,
    val lastMessageId: String? = null,
    val lastMessage: String? = null,
    val lastMessageType: MessageType? = null,
    val lastMessageTime: Long? = null,
    val draftMessage: String? = null,
    val backgroundPath: String? = null,
    val unreadCount: Int = 0,
    val isSending: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isHidden: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isOnline: Boolean = false
)