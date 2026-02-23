package top.chengdongqing.wechat.features.chat.data.mapper

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.MessageType
import top.chengdongqing.wechat.data.database.entity.SendError
import top.chengdongqing.wechat.data.database.entity.SendStatus
import top.chengdongqing.wechat.features.call.domain.model.CallStatus
import top.chengdongqing.wechat.features.call.domain.model.CallType
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
    draftMessage = draftMessage,
    backgroundPath = backgroundPath
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
    backgroundPath = backgroundPath,
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
        sendStatus = sendStatus.toDomain(this),
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
                localPath = localPath ?: "",
                mimeType = data.mimeType,
                filename = data.filename,
                width = data.width,
                height = data.height,
                size = fileSize ?: 0
            )
        }

        MessageType.Video -> {
            val data = runCatching {
                json.decodeFromString<MediaContent>(content)
            }.getOrElse { MediaContent() }

            MessageContent.Video(
                localPath = localPath ?: "",
                mimeType = data.mimeType,
                filename = data.filename,
                width = data.width,
                height = data.height,
                size = fileSize ?: 0,
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
                size = fileSize ?: 0,
            )
        }

        MessageType.Sticker ->
            MessageContent.Sticker(
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

        MessageType.VoiceCall,
        MessageType.VideoCall ->
            MessageContent.Call(
                type = if (contentType == MessageType.VideoCall) CallType.Video else CallType.Voice,
                status = runCatching {
                    CallStatus.valueOf(content)
                }.getOrDefault(CallStatus.Missed),
                duration = mediaDuration
            )
    }
}

fun MessageContent.toEntity(
    messageId: String,
    sessionId: String,
    senderId: String,
    receiverId: String,
    timestamp: Long,
    json: Json
): MessageEntity {
    val content = this
    val now = System.currentTimeMillis()

    // 公共字段
    fun base(
        contentType: MessageType,
        contentValue: String,
        localPath: String? = null,
        fileSize: Long? = null,
        mediaDuration: Long? = null
    ) = MessageEntity(
        messageId = messageId,
        sessionId = sessionId,
        senderId = senderId,
        receiverId = receiverId,
        contentType = contentType,
        content = contentValue,
        localPath = localPath,
        fileSize = fileSize,
        mediaDuration = mediaDuration,
        timestamp = timestamp,
        sendStatus = SendStatus.Sending,
        isFromMe = true,
        createdAt = now,
        updatedAt = now
    )

    return when (content) {
        is MessageContent.Text ->
            base(
                contentType = MessageType.Text,
                contentValue = content.text
            )

        is MessageContent.Voice ->
            base(
                contentType = MessageType.Voice,
                contentValue = "",
                localPath = content.localPath,
                mediaDuration = content.duration
            )

        is MessageContent.Image ->
            base(
                contentType = MessageType.Image,
                contentValue = json.encodeToString(
                    MediaContent(
                        width = content.width,
                        height = content.height,
                        mimeType = content.mimeType,
                        filename = content.filename
                    )
                ),
                localPath = content.localPath,
                fileSize = content.size
            )

        is MessageContent.Video ->
            base(
                contentType = MessageType.Video,
                contentValue = json.encodeToString(
                    MediaContent(
                        width = content.width,
                        height = content.height,
                        mimeType = content.mimeType,
                        filename = content.filename
                    )
                ),
                localPath = content.localPath,
                fileSize = content.size,
                mediaDuration = content.duration
            )

        is MessageContent.File ->
            base(
                contentType = MessageType.File,
                contentValue = json.encodeToString(
                    FileContent(
                        mimeType = content.mimeType,
                        filename = content.filename
                    )
                ),
                localPath = content.localPath,
                fileSize = content.size
            )

        is MessageContent.Sticker ->
            base(
                contentType = MessageType.Sticker,
                contentValue = content.localPath,
                localPath = content.localPath
            )

        is MessageContent.Location ->
            base(
                contentType = MessageType.Location,
                contentValue = json.encodeToString(
                    LocationContent(
                        latitude = content.latitude,
                        longitude = content.longitude,
                        address = content.address,
                        poiName = content.poiName
                    )
                ),
                localPath = content.snapshotPath
            )

        is MessageContent.ContactCard ->
            base(
                contentType = MessageType.ContactCard,
                contentValue = json.encodeToString(
                    ContactCardContent(
                        userId = content.userId,
                        name = content.name,
                        avatar = content.avatar
                    )
                )
            )

        is MessageContent.Favorite ->
            base(
                contentType = MessageType.Favorite,
                contentValue = json.encodeToString(
                    FavoriteContent(
                        title = content.title,
                        source = content.source
                    )
                ),
                localPath = content.previewPath
            )

        is MessageContent.Call ->
            base(
                contentType = if (content.type.isVideoCall) MessageType.VideoCall else MessageType.VoiceCall,
                contentValue = content.status.name,
                mediaDuration = content.duration
            )

        else -> base(
            contentType = MessageType.Text,
            contentValue = ""
        )
    }
}

// ==================== SendStatus ====================

fun SendStatus.toDomain(entity: MessageEntity): MessageSendStatus {
    return when (this) {
        SendStatus.Sending -> {
            val progress = entity.fileSize?.let { fileSize ->
                val total = fileSize.coerceAtLeast(1L)
                val sent = entity.sentBytes.coerceAtMost(total)
                sent.toFloat() / total.toFloat()
            } ?: 0f

            MessageSendStatus.Sending(progress)
        }

        SendStatus.Sent -> MessageSendStatus.Success
        SendStatus.Delivered -> MessageSendStatus.Success
        SendStatus.Read -> MessageSendStatus.Success
        SendStatus.Failed -> MessageSendStatus.Failed(entity.failReason ?: SendError.Unknown)
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