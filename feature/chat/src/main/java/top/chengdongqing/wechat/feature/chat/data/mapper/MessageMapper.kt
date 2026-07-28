package top.chengdongqing.wechat.feature.chat.data.mapper

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.common.call.CallStatus
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.data.model.MusicTrack
import top.chengdongqing.wechat.core.database.entity.MessageEntity
import top.chengdongqing.wechat.core.model.CallType
import top.chengdongqing.wechat.core.model.MessageSendStatus
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.model.SendError
import top.chengdongqing.wechat.core.model.SendStatus

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

        MessageType.Music -> {
            val music = runCatching {
                json.decodeFromString<MusicTrack>(content)
            }.recoverCatching {
                MusicTrack.valueOf(content)
            }.getOrDefault(MusicTrack.Perfect)
            MessageContent.Music(
                if (localPath != null && localPath != music.audioPath) {
                    music.copy(audioPath = localPath, coverPath = null)
                } else music
            )
        }

        MessageType.Live -> {
            val live = runCatching {
                json.decodeFromString<LiveContent>(content)
            }.getOrElse { LiveContent() }
            MessageContent.Live(
                liveId = live.liveId,
                title = live.title,
                hostName = live.hostName,
                status = live.status,
                actorId = live.actorId,
                targetId = live.targetId,
                payload = live.payload
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

        is MessageContent.Music ->
            base(
                contentValue = json.encodeToString(content.music),
                localPath = content.music.audioPath,
                fileSize = content.music.size.takeIf { it > 0 }
            )

        is MessageContent.Live ->
            base(
                contentValue = json.encodeToString(
                    LiveContent(
                        liveId = content.liveId,
                        title = content.title,
                        hostName = content.hostName,
                        status = content.status,
                        actorId = content.actorId,
                        targetId = content.targetId,
                        payload = content.payload
                    )
                )
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
    is MessageContent.ContactCard -> MessageType.ContactCard
    is MessageContent.Music -> MessageType.Music
    is MessageContent.Live -> MessageType.Live
    is MessageContent.Media -> MessageType.Image
}

@Serializable
private data class LiveContent(
    val liveId: String = "",
    val title: String = "群直播",
    val hostName: String = "",
    val status: String = "live",
    val actorId: String? = null,
    val targetId: String? = null,
    val payload: String? = null
)

fun MessageContent.getLocalPath(): String? = when (this) {
    is MessageContent.Voice -> localPath
    is MessageContent.Media -> localPath
    is MessageContent.File -> localPath
    is MessageContent.Sticker -> localPath
    is MessageContent.Music -> music.audioPath
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
        SendStatus.Read -> MessageSendStatus.Delivered

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
