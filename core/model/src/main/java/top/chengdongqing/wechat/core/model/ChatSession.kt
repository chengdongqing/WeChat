package top.chengdongqing.wechat.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class ChatSession(
    val id: String,
    val contactId: String,
    val contactName: String,
    val contactAvatar: String? = null,
    val lastMessageId: String? = null,
    val lastMessage: String? = null,
    val lastMessageType: MessageType? = null,
    val lastMessageTime: Long? = null,
    val lastMessageRecalled: Boolean = false,
    val lastMessageFromMe: Boolean = true,
    val draftMessage: String? = null,
    val backgroundPath: String? = null,
    val unreadCount: Int = 0,
    val isSending: Boolean = false,
    val isPinned: Boolean = false,
    val isBottomed: Boolean = false,
    val isMuted: Boolean = false,
    val isHidden: Boolean = false,
    val isTemporary: Boolean = false,
    val expiresAt: Long? = null,
    val temporaryPeerPublicKey: String? = null,
    val isOnline: Boolean = false
)
