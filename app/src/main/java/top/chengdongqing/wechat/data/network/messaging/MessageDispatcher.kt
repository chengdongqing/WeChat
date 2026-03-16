package top.chengdongqing.wechat.data.network.messaging

import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.util.downloadAvatar
import top.chengdongqing.wechat.core.util.extractExtension
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.model.MessageType
import top.chengdongqing.wechat.data.model.SendError
import top.chengdongqing.wechat.data.model.SendStatus
import top.chengdongqing.wechat.data.network.model.ChatProtocol
import top.chengdongqing.wechat.data.network.model.ReceiptType
import top.chengdongqing.wechat.features.call.manager.SignalingManager
import top.chengdongqing.wechat.features.chat.data.mapper.MediaContent
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.repository.MessageRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息分发器
 *
 * 纯业务逻辑，不感知传输细节
 */
@Singleton
class MessageDispatcher @Inject constructor(
    private val messageRepository: MessageRepository,
    private val privateFileManager: PrivateFileManager,
    private val signalingManager: SignalingManager,
    private val contactRepository: ContactRepository,
    private val json: Json
) {
    private companion object {
        const val TAG = "MessageDispatcher"
    }

    private val _incomingMessages = MutableSharedFlow<ChatMessage>(
        replay = 0, // 不缓存历史消息，订阅者只收到订阅后的新消息
        extraBufferCapacity = 64 // 突发消息不阻塞发送方协程
    )

    /**
     * 新消息流，供上层发送通知等
     */
    val incomingMessages: Flow<ChatMessage> = _incomingMessages.asSharedFlow()

    /**
     * 分发 JSON 类协议包（文本、回执、信令、心跳等）
     */
    suspend fun dispatch(protocol: ChatProtocol) {
        runCatching {
            when (protocol) {
                is ChatProtocol.TextMessage -> handleTextMessage(protocol)
                is ChatProtocol.CallMessage -> handleCallMessage(protocol)
                is ChatProtocol.MessageReceipt -> handleReceipt(protocol)
                is ChatProtocol.Signaling -> handleSignaling(protocol)
                is ChatProtocol.ProfileResponse -> updateContactProfile(protocol)
                else -> Unit
            }
        }.onFailure {
            Log.e(TAG, "分发失败: ${protocol::class.simpleName}", it)
        }
    }

    /**
     * 分发媒体消息
     */
    suspend fun dispatch(protocol: ChatProtocol.MediaMessage, tempFile: File) {
        runCatching {
            handleMediaMessage(protocol, tempFile)
        }.onFailure {
            Log.e(TAG, "分发媒体消息失败: ${protocol.messageId}", it)
            tempFile.delete()
        }
    }

    private suspend fun handleTextMessage(protocol: ChatProtocol.TextMessage) {
        handleIncomingChat(protocol) { createTextEntity(protocol) }
    }

    private suspend fun handleCallMessage(protocol: ChatProtocol.CallMessage) {
        handleIncomingChat(protocol) { createCallEntity(protocol) }
    }

    private suspend fun handleMediaMessage(protocol: ChatProtocol.MediaMessage, tempFile: File) {
        handleIncomingChat(protocol) { createMediaEntity(protocol, tempFile) }
    }

    /**
     * 聊天消息通用处理流程
     *
     * 1. 幂等检查：已存在则只补发 ACK
     * 2. 构建实体并入库
     * 3. 更新会话（未读数、最新消息等）
     * 4. 发送送达回执
     * 5. 推送到消息流触发通知
     */
    private suspend fun handleIncomingChat(
        protocol: ChatProtocol,
        entityBuilder: suspend () -> MessageEntity
    ) {
        messageRepository.handleIncomingMessage(
            protocol,
            entityBuilder,
        ) { message ->
            _incomingMessages.emit(message)
        }
    }

    /**
     * 处理回执消息
     */
    private suspend fun handleReceipt(protocol: ChatProtocol.MessageReceipt) {
        val messageId = protocol.messageId

        runCatching {
            when (val type = protocol.receiptType) {
                // 送达/已读
                ReceiptType.Delivered,
                ReceiptType.Read -> {
                    val status = if (type == ReceiptType.Delivered) {
                        SendStatus.Delivered
                    } else {
                        SendStatus.Read
                    }
                    messageRepository.updateMessageStatus(
                        messageId = messageId,
                        status = status
                    )
                }

                // 撤回消息
                ReceiptType.Recalled -> {
                    messageRepository.recallMessage(protocol.messageId)
                }

                // 拒收
                ReceiptType.Blocked,
                ReceiptType.NotFriend,
                ReceiptType.InvalidSignature -> {
                    val failedReason = when (type) {
                        ReceiptType.Blocked -> SendError.Blocked
                        ReceiptType.NotFriend -> SendError.NotFriend
                        ReceiptType.InvalidSignature -> SendError.Unknown
                    }
                    messageRepository.updateMessageStatus(
                        messageId = messageId,
                        status = SendStatus.Failed,
                        failedReason = failedReason
                    )
                }
            }
        }.onFailure {
            Log.e(TAG, "回执处理失败: $messageId", it)
        }
    }

    /**
     * 转发 WebRTC 信令给 SignalingManager 处理（Offer/Answer/ICE/Hangup 等）
     */
    private suspend fun handleSignaling(protocol: ChatProtocol.Signaling) {
        signalingManager.onSignalingReceived(protocol)
    }

    /**
     * 更新联系人资料
     */
    private suspend fun updateContactProfile(protocol: ChatProtocol.ProfileResponse) {
        val newProfile = protocol.profile
        val userId = newProfile.userId

        // 查询旧头像
        val oldAvatarPath = contactRepository.getContact(userId)?.avatarPath

        // 下载新头像
        val newAvatarPath = newProfile.avatarUrl?.let { url ->
            val file = File.createTempFile("IMG_", ".jpg")
            downloadAvatar(url, file).getOrNull()?.let {
                privateFileManager.saveAvatar(userId, file.toUri()).getOrNull()
            }
        }

        // 更新联系人资料
        contactRepository.updateContact(newProfile.userId) { contact ->
            contact.copy(
                avatarPath = newAvatarPath ?: contact.avatarPath,
                nickname = newProfile.nickname,
                signature = newProfile.signature,
                gender = newProfile.gender,
                version = System.currentTimeMillis() // 更新版本号
            )
        }

        // 删除旧文件
        if (newAvatarPath != null && oldAvatarPath != null) {
            privateFileManager.deleteFile(oldAvatarPath)
        }
    }

    /**
     * 构建文本消息实体
     */
    private fun createTextEntity(protocol: ChatProtocol.TextMessage): MessageEntity {
        return MessageEntity(
            id = protocol.messageId,
            sessionId = protocol.senderId,
            senderId = protocol.senderId,
            receiverId = protocol.receiverId,
            contentType = protocol.messageType,
            content = protocol.content,
            timestamp = protocol.timestamp,
            sendStatus = SendStatus.Delivered,
            isFromMe = false
        )
    }

    /**
     * 构建通话记录实体
     */
    private fun createCallEntity(protocol: ChatProtocol.CallMessage): MessageEntity {
        val contentType = if (protocol.callType.isVideoCall) {
            MessageType.VideoCall
        } else {
            MessageType.VoiceCall
        }

        return MessageEntity(
            id = protocol.messageId,
            sessionId = protocol.senderId,
            senderId = protocol.senderId,
            receiverId = protocol.receiverId,
            contentType = contentType,
            content = protocol.status,
            mediaDuration = protocol.duration,
            timestamp = protocol.timestamp,
            sendStatus = SendStatus.Delivered,
            isFromMe = false
        )
    }

    /**
     * 构建媒体消息实体
     *
     * 文件名来源：部分类型（如文件）的文件名嵌在 content JSON 里，其余类型直接用 content 作为文件名。
     * 持久化成功后删除临时文件；失败时降级用 content 作为 localPath 并保留临时文件供排查。
     */
    private suspend fun createMediaEntity(
        protocol: ChatProtocol.MediaMessage,
        tempFile: File
    ): MessageEntity {
        val filename = if (protocol.messageType.isFileNameInJson) {
            json.decodeFromString<MediaContent>(protocol.content).filename
        } else {
            protocol.content
        }

        val localPath = privateFileManager.saveMedia(
            messageType = protocol.messageType,
            sourceFile = tempFile,
            extension = filename.extractExtension()
        ).also {
            tempFile.delete()
        }.getOrElse {
            Log.e(TAG, "保存媒体文件失败: ${protocol.messageId}", it)
            protocol.content
        }

        return MessageEntity(
            id = protocol.messageId,
            sessionId = protocol.senderId,
            senderId = protocol.senderId,
            receiverId = protocol.receiverId,
            contentType = protocol.messageType,
            content = protocol.content,
            localPath = localPath,
            fileSize = protocol.fileSize,
            mediaDuration = protocol.mediaDuration,
            timestamp = protocol.timestamp,
            sendStatus = SendStatus.Delivered,
            isFromMe = false
        )
    }
}