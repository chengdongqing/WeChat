package top.chengdongqing.wechat.features.chat.data.mapper

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.model.MessageType
import top.chengdongqing.wechat.data.model.SendError
import top.chengdongqing.wechat.data.model.SendStatus
import top.chengdongqing.wechat.features.call.model.CallStatus
import top.chengdongqing.wechat.features.call.model.CallType
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.domain.model.MessageSendStatus
import top.chengdongqing.wechat.features.chat.domain.model.MusicTrack

fun MessageEntity.toDomain(json: Json): ChatMessage {
    return ChatMessage(
        id = id,
        sessionId = sessionId,
        senderId = senderId,
        content = toMessageContent(json),
        isFromMe = isFromMe,
        isRecalled = isRecalled,
        timestamp = timestamp,
        sendStatus = sendStatus.toDomain(this)
    )
}

private fun MessageEntity.toMessageContent(json: Json): MessageContent {
    return when (contentType) {
        MessageType.Text ->
            MessageContent.Text(content)

        MessageType.Voice ->
            MessageContent.Voice(
                localPath = localPath ?: "",
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
                localPath = localPath ?: "",
                filename = data.filename,
                mimeType = data.mimeType,
                size = fileSize ?: 0,
            )
        }

        MessageType.Sticker ->
            MessageContent.Sticker(
                localPath = localPath!!
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
            }.getOrElse { ContactCardContent() }

            MessageContent.ContactCard(
                userId = cardInfo.userId,
                nickname = cardInfo.nickname,
                avatarPath = localPath ?: ""
            )
        }

        MessageType.Favorite -> {
            MessageContent.Favorite(
                title = "",
                source = "",
                previewPath = localPath
            )
        }

        MessageType.Music -> {
            val music = runCatching {
                MusicTrack.valueOf(content)
            }.getOrDefault(MusicTrack.Perfect)
            MessageContent.Music(music)
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

    // 公共字段
    fun base(
        contentValue: String,
        localPath: String? = null,
        fileSize: Long? = null,
        mediaDuration: Long? = null
    ) = MessageEntity(
        id = messageId,
        sessionId = sessionId,
        senderId = senderId,
        receiverId = receiverId,
        contentType = toMessageType(),
        content = contentValue,
        localPath = localPath,
        fileSize = fileSize,
        mediaDuration = mediaDuration,
        isFromMe = true,
        timestamp = timestamp,
        sendStatus = SendStatus.Sending,
    )

    return when (content) {
        is MessageContent.Text ->
            base(
                contentValue = content.text
            )

        is MessageContent.Voice ->
            base(
                contentValue = content.localPath,
                localPath = content.localPath,
                mediaDuration = content.duration
            )

        is MessageContent.Image ->
            base(
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
                contentValue = content.localPath,
                localPath = content.localPath
            )

        is MessageContent.Location ->
            base(
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
                contentValue = json.encodeToString(
                    ContactCardContent(
                        userId = content.userId,
                        nickname = content.nickname
                    )
                ),
                localPath = content.avatarPath
            )

        is MessageContent.Favorite ->
            base(
                contentValue = "",
                localPath = content.previewPath
            )

        is MessageContent.Music ->
            base(
                contentValue = content.music.name
            )

        is MessageContent.Call ->
            base(
                contentValue = content.status.name,
                mediaDuration = content.duration
            )

        else -> base(
            contentValue = ""
        )
    }
}

fun MessageContent.toMessageType(): MessageType = when (this) {
    is MessageContent.Text -> MessageType.Text
    is MessageContent.Image -> MessageType.Image
    is MessageContent.Video -> MessageType.Video
    is MessageContent.Voice -> MessageType.Voice
    is MessageContent.Sticker -> MessageType.Sticker
    is MessageContent.File -> MessageType.File
    is MessageContent.Location -> MessageType.Location
    is MessageContent.Call -> if (type.isVideoCall) MessageType.VideoCall else MessageType.VoiceCall
    is MessageContent.Favorite -> MessageType.Favorite
    is MessageContent.ContactCard -> MessageType.ContactCard
    is MessageContent.Music -> MessageType.Music
    is MessageContent.Media -> MessageType.Image
}

fun MessageContent.getLocalPath(): String? = when (this) {
    is MessageContent.Voice -> localPath
    is MessageContent.Media -> localPath
    is MessageContent.File -> localPath
    is MessageContent.Sticker -> localPath
    else -> null
}

fun SendStatus.toDomain(entity: MessageEntity): MessageSendStatus {
    val progress = entity.fileSize?.let { fileSize ->
        val total = fileSize.coerceAtLeast(1L)
        val sent = entity.sentBytes.coerceAtMost(total)
        sent.toFloat() / total.toFloat()
    } ?: 0f

    return when (this) {
        SendStatus.Sending -> MessageSendStatus.Sending(progress)
        SendStatus.Receiving -> MessageSendStatus.Receiving(progress)
        SendStatus.Paused -> MessageSendStatus.Paused(progress)
        SendStatus.Sent -> MessageSendStatus.Sent
        SendStatus.Delivered,
        SendStatus.Read -> MessageSendStatus.Success

        SendStatus.Failed -> MessageSendStatus.Failed(entity.failReason ?: SendError.Unknown)
    }
}

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
    val userId: String = "",
    val nickname: String = ""
)