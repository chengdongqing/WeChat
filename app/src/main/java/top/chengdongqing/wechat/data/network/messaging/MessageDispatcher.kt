package top.chengdongqing.wechat.data.network.messaging

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.MediaFileDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.model.MessageType
import top.chengdongqing.wechat.data.model.SendError
import top.chengdongqing.wechat.data.model.SendStatus
import top.chengdongqing.wechat.data.network.model.ChatProtocol
import top.chengdongqing.wechat.data.network.model.ReceiptType
import top.chengdongqing.wechat.data.session.FileReferenceManager
import top.chengdongqing.wechat.features.call.manager.SignalingManager
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.repository.MessageRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息分发器
 */
@Singleton
class MessageDispatcher @Inject constructor(
    private val messageRepository: MessageRepository,
    private val privateFileManager: PrivateFileManager,
    private val signalingManager: SignalingManager,
    private val contactRepository: ContactRepository,
    private val mediaFileDao: MediaFileDao,
    private val fileReferenceManager: FileReferenceManager,
    private val messageDao: MessageDao,
    private val database: WeDatabase,
    private val chatSessionUpdater: ChatSessionUpdater
) {
    private companion object {
        const val TAG = "MessageDispatcher"
    }

    private val _incomingMessages = MutableSharedFlow<ChatMessage>(
        replay = 0, // 不缓存历史消息，订阅者只收到订阅后的新消息
        extraBufferCapacity = 64 // 突发消息不阻塞发送方协程
    )

    val incomingMessages: Flow<ChatMessage> = _incomingMessages.asSharedFlow()

    /**
     * 分发 JSON 类协议包（文本、回执、信令、心跳等）
     */
    suspend fun dispatch(protocol: ChatProtocol) = runCatching {
        when (protocol) {
            is ChatProtocol.TextMessage -> handleTextMessage(protocol)
            is ChatProtocol.CallMessage -> handleCallMessage(protocol)
            is ChatProtocol.MessageReceipt -> handleReceipt(protocol)
            is ChatProtocol.Signaling -> handleSignaling(protocol)
            is ChatProtocol.ProfileResponse -> contactRepository.syncContactProfile(protocol)
            else -> Unit
        }
    }.onFailure {
        Log.e(TAG, "分发失败: ${protocol::class.simpleName}", it)
    }

    /**
     * 分发接收完成的媒体消息
     *
     * 两种情况：
     * - 大文件：FILE_META 阶段已通过 [createReceivingMessage] 创建了 Receiving 状态的记录，
     *   这里只需要持久化文件并更新为 Delivered。
     *   送达回执由调用方（MessageReceiver）在此方法返回后单独发送。
     * - 小文件：没有预创建记录，走完整入库流程（insert + 更新会话 + 发回执 + 通知）
     */
    suspend fun dispatch(protocol: ChatProtocol.MediaMessage, tempFile: File) = runCatching {
        // 持久化文件
        val localPath = resolveLocalPath(protocol, tempFile).getOrThrow()

        if (messageDao.exists(protocol.messageId)) {
            // 大文件：已有 Receiving 记录，更新为 Delivered
            handleMediaComplete(protocol, localPath)
        } else {
            // 小文件：没有预创建记录，走完整入库流程
            handleMediaInsert(protocol, localPath)
        }
    }.onFailure {
        Log.e(TAG, "分发媒体消息失败: ${protocol.messageId}", it)
        tempFile.delete()
    }

    /**
     * 分发已存在的媒体消息（checksum 命中，无需传输文件）
     *
     * 直接创建 Delivered 状态的消息实体
     */
    suspend fun dispatchExistingMedia(
        protocol: ChatProtocol.MediaMessage,
        existingLocalPath: String
    ) = runCatching {
        handleIncomingChat(protocol) {
            MessageEntity(
                id = protocol.messageId,
                sessionId = protocol.senderId,
                senderId = protocol.senderId,
                receiverId = protocol.receiverId,
                contentType = protocol.messageType,
                content = protocol.content,
                localPath = existingLocalPath,
                fileSize = protocol.fileSize,
                mediaDuration = protocol.mediaDuration,
                timestamp = protocol.timestamp,
                sendStatus = SendStatus.Delivered,
                isFromMe = false
            )
        }
    }.onFailure {
        Log.e(TAG, "分发已存在媒体消息失败: ${protocol.messageId}", it)
    }

    /**
     * 创建 Receiving 状态的消息占位
     *
     * 在 FILE_META 协商阶段调用，让 UI 能看到正在接收的文件及进度。
     */
    suspend fun createReceivingMessage(metadata: ChatProtocol.MediaMessage) = runCatching {
        if (messageDao.exists(metadata.messageId)) return@runCatching

        val entity = MessageEntity(
            id = metadata.messageId,
            sessionId = metadata.senderId,
            senderId = metadata.senderId,
            receiverId = metadata.receiverId,
            contentType = metadata.messageType,
            content = metadata.content,
            localPath = null, // 文件尚未接收完成
            fileSize = metadata.fileSize,
            mediaDuration = metadata.mediaDuration,
            timestamp = metadata.timestamp,
            sendStatus = SendStatus.Receiving,
            isFromMe = false
        )

        database.withTransaction {
            messageDao.insert(entity)
            chatSessionUpdater.update(entity)
        }

        // 不发送送达回执、不调用 sendAck、不推送通知流（正在接收中的文件不需要通知）
    }.onFailure {
        Log.e(TAG, "创建接收消息失败: ${metadata.messageId}", it)
    }

    /**
     * 更新接收进度（大文件专用）
     *
     * @param receivedBytes 已接收字节数；-1 表示传输被取消
     */
    suspend fun updateReceiveProgress(messageId: String, receivedBytes: Long) = runCatching {
        if (receivedBytes < 0) {
            // 传输取消
            messageDao.update(messageId) { message ->
                message.copy(
                    sendStatus = SendStatus.Failed,
                    failReason = SendError.Cancelled
                )
            }
        } else {
            messageDao.update(messageId) { message ->
                message.copy(sentBytes = receivedBytes)
            }
        }
    }.onFailure {
        Log.w(TAG, "更新接收进度失败: $messageId", it)
    }

    private suspend fun handleTextMessage(protocol: ChatProtocol.TextMessage) {
        handleIncomingChat(protocol) { createTextEntity(protocol) }
    }

    private suspend fun handleCallMessage(protocol: ChatProtocol.CallMessage) {
        handleIncomingChat(protocol) { createCallEntity(protocol) }
    }

    /**
     * 媒体文件接收完成后的处理
     */
    private suspend fun handleMediaComplete(
        protocol: ChatProtocol.MediaMessage,
        localPath: String
    ) {
        messageDao.update(protocol.messageId) { message ->
            message.copy(
                localPath = localPath,
                sendStatus = SendStatus.Delivered,
                sentBytes = protocol.fileSize
            )
        }
    }

    /**
     * 小文件接收完成：走完整入库流程（insert + 更新会话 + 发回执 + 通知）
     */
    private suspend fun handleMediaInsert(
        protocol: ChatProtocol.MediaMessage,
        localPath: String
    ) {
        handleIncomingChat(protocol) {
            MessageEntity(
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

    /**
     * 聊天消息通用入库流程
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
     * 处理回执
     */
    private suspend fun handleReceipt(protocol: ChatProtocol.MessageReceipt) = runCatching {
        val messageId = protocol.messageId

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
                messageRepository.recallMessage(messageId)
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
        Log.e(TAG, "回执处理失败: ${protocol.messageId}", it)
    }

    private suspend fun handleSignaling(protocol: ChatProtocol.Signaling) {
        signalingManager.onSignalingReceived(protocol)
    }

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
     * 解析本地文件路径：复用已存在文件或持久化临时文件
     */
    private suspend fun resolveLocalPath(
        protocol: ChatProtocol.MediaMessage,
        tempFile: File
    ): Result<String> = runCatching {
        val existingFile = mediaFileDao.getByChecksum(protocol.checksum)

        if (existingFile != null) {
            fileReferenceManager.retain(existingFile.localPath, protocol.checksum)
            existingFile.localPath
        } else {
            val newPath = privateFileManager.saveMedia(
                messageType = protocol.messageType,
                sourceFile = tempFile,
                extension = protocol.extension
            ).getOrThrow()
            fileReferenceManager.retain(newPath, protocol.checksum)
            newPath
        }
    }.also {
        tempFile.takeIf { it.exists() }?.delete()
    }
}