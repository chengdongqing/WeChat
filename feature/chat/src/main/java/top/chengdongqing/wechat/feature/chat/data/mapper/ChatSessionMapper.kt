package top.chengdongqing.wechat.feature.chat.data.mapper

import top.chengdongqing.wechat.core.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.core.model.ChatSession

fun ChatSessionEntity.toDomain() = ChatSession(
    id = id,
    contactId = contactId,
    contactName = contactName,
    contactAvatar = contactAvatar,
    lastMessageId = lastMessageId,
    lastMessage = lastMessage,
    lastMessageType = lastMessageType,
    lastMessageTime = lastMessageTime,
    lastMessageRecalled = lastMessageRecalled,
    lastMessageFromMe = lastMessageFromMe,
    unreadCount = unreadCount,
    isSending = isSending,
    isPinned = isPinned,
    isBottomed = isBottomed,
    isMuted = isMuted,
    isHidden = isHidden,
    isTemporary = isTemporary,
    expiresAt = expiresAt,
    temporaryPeerPublicKey = temporaryPeerPublicKey,
    draftMessage = draftMessage,
    backgroundPath = backgroundPath
)

fun ChatSession.toEntity() = ChatSessionEntity(
    id = id,
    contactId = contactId,
    contactName = contactName,
    contactAvatar = contactAvatar,
    lastMessageId = lastMessageId,
    lastMessage = lastMessage,
    lastMessageType = lastMessageType,
    lastMessageTime = lastMessageTime,
    lastMessageRecalled = lastMessageRecalled,
    lastMessageFromMe = lastMessageFromMe,
    unreadCount = unreadCount,
    isSending = isSending,
    isPinned = isPinned,
    isBottomed = isBottomed,
    isMuted = isMuted,
    isHidden = isHidden,
    isTemporary = isTemporary,
    expiresAt = expiresAt,
    temporaryPeerPublicKey = temporaryPeerPublicKey,
    draftMessage = draftMessage,
    backgroundPath = backgroundPath
)
