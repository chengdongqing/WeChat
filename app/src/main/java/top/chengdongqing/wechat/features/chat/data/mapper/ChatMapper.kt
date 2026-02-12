package top.chengdongqing.wechat.features.chat.data.mapper

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.MessageType
import top.chengdongqing.wechat.data.database.entity.SendStatus
import top.chengdongqing.wechat.features.chat.domain.model.CallStatus
import top.chengdongqing.wechat.features.chat.domain.model.CallType
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.ChatSession
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.domain.model.MessageSendStatus

// ==================== ChatSession ====================

fun ChatSessionEntity.toDomain() = ChatSession(
    sessionId = sessionId,
    contactId = contactId,
    contactName = contactName,
    contactAvatar = contactAvatar,
    lastMessage = lastMessage,
    lastMessageType = lastMessageType,
    lastMessageTime = lastMessageTime,
    unreadCount = unreadCount,
    isPinned = isPinned,
    isMuted = isMuted,
    draftMessage = draftMessage
)

fun ChatSession.toEntity() = ChatSessionEntity(
    sessionId = sessionId,
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
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)

// ==================== Message ====================

fun MessageEntity.toDomain(json: Json): ChatMessage {
    return ChatMessage(
        id = messageId,
        sessionId = sessionId,
        senderId = senderId,
        content = toMessageContent(json),
        isFromMe = isFromMe,
        timestamp = timestamp,
        sendStatus = sendStatus.toDomain(failReason),
        retryCount = retryCount
    )
}

private fun MessageEntity.toMessageContent(json: Json): MessageContent {
    return when (contentType) {
        MessageType.Text ->
            MessageContent.Text(content)

        MessageType.Voice ->
            MessageContent.Voice(
                localPath = localPath!!,
                duration = mediaDuration ?: 0,
                isPlayed = isPlayed
            )

        MessageType.Image -> {
            val data = runCatching {
                json.decodeFromString<MediaContent>(content)
            }.getOrElse { MediaContent() }

            MessageContent.Image(
                localPath = localPath!!,
                mimeType = data.mimeType,
                filename = data.filename,
                width = data.width,
                height = data.height,
                size = mediaSize ?: 0
            )
        }

        MessageType.Video -> {
            val data = runCatching {
                json.decodeFromString<MediaContent>(content)
            }.getOrElse { MediaContent() }

            MessageContent.Video(
                localPath = localPath!!,
                mimeType = data.mimeType,
                filename = data.filename,
                width = data.width,
                height = data.height,
                size = mediaSize ?: 0,
                duration = mediaDuration ?: 0
            )
        }

        MessageType.File -> {
            val data = runCatching {
                json.decodeFromString<FileContent>(content)
            }.getOrElse { FileContent() }

            MessageContent.File(
                localPath = localPath!!,
                filename = data.filename,
                mimeType = data.mimeType,
                size = mediaSize ?: 0,
            )
        }

        MessageType.Sticker ->
            MessageContent.Sticker(
                stickerId = content,
                localPath = localPath ?: ""
            )

        MessageType.Location -> {
            val data = runCatching {
                json.decodeFromString<LocationContent>(content)
            }.getOrElse { LocationContent() }

            MessageContent.Location(
                latitude = data.latitude,
                longitude = data.longitude,
                address = data.address,
                poiName = data.poiName,
                snapshotPath = localPath
            )
        }

        MessageType.ContactCard -> {
            val cardInfo = runCatching {
                Json.decodeFromString<ContactCardContent>(content)
            }.getOrNull()
            MessageContent.ContactCard(
                userId = cardInfo?.userId ?: "",
                name = cardInfo?.name ?: "",
                avatar = cardInfo?.avatar ?: ""
            )
        }

        MessageType.Favorite -> {
            val favoriteInfo = runCatching {
                Json.decodeFromString<FavoriteContent>(content)
            }.getOrNull()
            MessageContent.Favorite(
                title = favoriteInfo?.title ?: "",
                source = favoriteInfo?.source ?: "",
                previewPath = localPath
            )
        }

        MessageType.VoiceCall ->
            MessageContent.Call(
                type = CallType.Voice,
                status = runCatching {
                    CallStatus.valueOf(content)
                }.getOrDefault(CallStatus.Missed),
                duration = mediaDuration
            )

        MessageType.VideoCall ->
            MessageContent.Call(
                type = CallType.Video,
                status = runCatching {
                    CallStatus.valueOf(content)
                }.getOrDefault(CallStatus.Missed),
                duration = mediaDuration
            )
    }
}

// ==================== SendStatus ====================

fun SendStatus.toDomain(failReason: String? = null): MessageSendStatus {
    return when (this) {
        SendStatus.Sending -> MessageSendStatus.Sending()
        SendStatus.Sent -> MessageSendStatus.Success
        SendStatus.Delivered -> MessageSendStatus.Success
        SendStatus.Read -> MessageSendStatus.Success
        SendStatus.Failed -> when (failReason) {
            "OFFLINE" -> MessageSendStatus.Failed.RecipientOffline()
            "NOT_REACHABLE" -> MessageSendStatus.Failed.RecipientOffline()
            "NOT_FRIEND" -> MessageSendStatus.Failed.NotFriend()
            "BLOCKED" -> MessageSendStatus.Failed.Blocked()
            "TOO_LARGE" -> MessageSendStatus.Failed.MessageTooLarge()
            else -> MessageSendStatus.Failed.NetworkError()
        }
    }
}

fun MessageSendStatus.toEntity(): SendStatus {
    return when (this) {
        is MessageSendStatus.Sending -> SendStatus.Sending
        is MessageSendStatus.Paused -> SendStatus.Sending
        is MessageSendStatus.Success -> SendStatus.Sent
        is MessageSendStatus.Failed -> SendStatus.Failed
    }
}

// ==================== 辅助数据类（JSON 序列化用）====================

@Serializable
data class MediaContent(
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "",
    val filename: String = ""
)

@Serializable
data class FileContent(
    val filename: String = "",
    val mimeType: String = ""
)

@Serializable
data class LocationContent(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val poiName: String = ""
)

@Serializable
data class ContactCardContent(
    val userId: String,
    val name: String,
    val avatar: String
)

@Serializable
data class FavoriteContent(
    val title: String,
    val source: String
)