package top.chengdongqing.wechat.features.chat.data.mapper

import top.chengdongqing.wechat.data.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.features.chat.domain.model.ChatSession

fun ChatSessionEntity.toDomain() = ChatSession(
    id = id,
    contactId = contactId,
    contactName = contactName,
    contactAvatar = contactAvatar,
    lastMessageId = lastMessageId,
    lastMessage = lastMessage,
    lastMessageType = lastMessageType,
    lastMessageTime = lastMessageTime,
    unreadCount = unreadCount,
    isSending = isSending,
    isPinned = isPinned,
    isMuted = isMuted,
    isSpeakerOn = isSpeakerOn,
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
    unreadCount = unreadCount,
    isSending = isSending,
    isPinned = isPinned,
    isMuted = isMuted,
    isSpeakerOn = isSpeakerOn,
    draftMessage = draftMessage,
    backgroundPath = backgroundPath
)