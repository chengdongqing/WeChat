package top.chengdongqing.wechat.features.chat.data.mapper

import top.chengdongqing.wechat.data.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.features.chat.domain.model.ChatSession

fun ChatSessionEntity.toDomain() = ChatSession(
    sessionId = id,
    contactId = contactId,
    contactName = contactName,
    contactAvatar = contactAvatar,
    lastMessage = lastMessage,
    lastMessageType = lastMessageType,
    lastMessageTime = lastMessageTime,
    unreadCount = unreadCount,
    isPinned = isPinned,
    isMuted = isMuted,
    draftMessage = draftMessage,
    backgroundPath = backgroundPath
)

fun ChatSession.toEntity() = ChatSessionEntity(
    id = sessionId,
    contactId = contactId,
    contactName = contactName,
    contactAvatar = contactAvatar,
    lastMessage = lastMessage,
    lastMessageType = lastMessageType,
    lastMessageTime = lastMessageTime,
    unreadCount = unreadCount,
    isPinned = isPinned,
    isMuted = isMuted,
    draftMessage = draftMessage,
    backgroundPath = backgroundPath
)