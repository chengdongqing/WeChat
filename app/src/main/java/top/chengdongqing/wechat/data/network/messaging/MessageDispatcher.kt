package top.chengdongqing.wechat.data.network.messaging

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.manager.FileManager
import top.chengdongqing.wechat.core.util.extractFileExtension
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.MessageType
import top.chengdongqing.wechat.data.database.entity.SendError
import top.chengdongqing.wechat.data.database.entity.SendStatus
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.ReceiptType
import top.chengdongqing.wechat.data.network.transfer.TransferManager
import top.chengdongqing.wechat.features.call.manager.SignalingManager
import top.chengdongqing.wechat.features.chat.data.mapper.MediaContent
import top.chengdongqing.wechat.features.chat.data.mapper.toDomain
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
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
    private val messageDao: MessageDao,
    private val connectionInfoDao: ConnectionInfoDao,
    private val messageSender: MessageSender,
    private val chatSessionUpdater: ChatSessionUpdater,
    private val fileManager: FileManager,
    private val signalingManager: SignalingManager,
    private val transferManager: TransferManager,
    private val json: Json
) {
    private companion object {
        const val TAG = "MessageDispatcher"
    }

    /**
     * 新消息流，供上层发送通知/更新 UI
     *
     * replay=0：不缓存历史消息，订阅者只收到订阅后的新消息
     * extraBufferCapacity=64：突发消息不阻塞发送方协程
     */
    private val _incomingMessageFlow = MutableSharedFlow<ChatMessage>(
        replay = 0, extraBufferCapacity = 64
    )
    val incomingMessageFlow: SharedFlow<ChatMessage> = _incomingMessageFlow.asSharedFlow()

    // ==================== 分发入口 ====================

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
                is ChatProtocol.Handshake -> handleHeartbeat(protocol)
                else -> {}
            }
        }.onFailure { Log.e(TAG, "分发失败: ${protocol::class.simpleName}", it) }
    }

    /**
     * 分发媒体消息
     *
     * tempFile 由 [MessageReceiver] 传入，处理完成后由 [FileManager] 或异常处理负责删除。
     */
    suspend fun dispatch(protocol: ChatProtocol.MediaMessage, tempFile: File) {
        runCatching {
            handleMediaMessage(protocol, tempFile)
        }.onFailure {
            Log.e(TAG, "分发媒体消息失败: ${protocol.messageId}", it)
            tempFile.delete()
        }
    }

    // ==================== 聊天消息 ====================

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
        try {
            if (messageDao.exists(protocol.messageId)) {
                messageSender.sendReceipt(
                    protocol.messageId,
                    protocol.senderId,
                    ReceiptType.Delivered
                )
                return
            }

            val entity = entityBuilder()
            messageDao.insert(entity)
            chatSessionUpdater.update(entity)
            messageSender.sendReceipt(protocol.messageId, protocol.senderId, ReceiptType.Delivered)
            _incomingMessageFlow.emit(entity.toDomain(json))
        } catch (e: Exception) {
            Log.e(TAG, "处理消息失败: ${protocol.messageId}", e)
        }
    }

    // ==================== 回执 ====================

    /**
     * 处理回执消息
     */
    private suspend fun handleReceipt(protocol: ChatProtocol.MessageReceipt) {
        runCatching {
            when (protocol.receiptType) {
                ReceiptType.Delivered,
                ReceiptType.Read -> {
                    val status = if (protocol.receiptType == ReceiptType.Delivered) {
                        SendStatus.Delivered
                    } else {
                        SendStatus.Read
                    }
                    messageDao.updateSendStatus(protocol.messageId, status)
                }

                ReceiptType.Blocked,
                ReceiptType.NotFriend -> {
                    val reason = if (protocol.receiptType == ReceiptType.Blocked) {
                        SendError.Blocked
                    } else {
                        SendError.NotFriend
                    }
                    messageDao.updateSendStatusAndFailReason(
                        messageId = protocol.messageId,
                        status = SendStatus.Failed,
                        reason = reason
                    )
                    // 停止发送文件（如果有）
                    transferManager.setCancelled(protocol.messageId)
                }
            }
        }.onFailure { Log.e(TAG, "回执处理失败: ${protocol.messageId}", it) }
    }

    /** 转发 WebRTC 信令给 SignalingManager 处理（Offer/Answer/ICE/Hangup 等） */
    private suspend fun handleSignaling(protocol: ChatProtocol.Signaling) {
        signalingManager.onSignalingReceived(protocol)
    }

    /**
     * 收到心跳包，更新对方的在线时间戳
     */
    private suspend fun handleHeartbeat(protocol: ChatProtocol.Handshake) {
        runCatching { connectionInfoDao.markOnline(protocol.senderId, protocol.timestamp) }
            .onFailure { Log.e(TAG, "心跳处理失败: ${protocol.senderId}", it) }
    }

    // ==================== 实体构建 ====================

    /**
     * 构建文本消息实体
     */
    private fun createTextEntity(protocol: ChatProtocol.TextMessage): MessageEntity {
        val now = System.currentTimeMillis()

        return MessageEntity(
            messageId = protocol.messageId,
            sessionId = protocol.senderId,
            senderId = protocol.senderId,
            receiverId = protocol.receiverId,
            contentType = protocol.messageType,
            content = protocol.content,
            timestamp = protocol.timestamp,
            sendStatus = SendStatus.Delivered,
            isFromMe = false,
            createdAt = now,
            updatedAt = now
        )
    }

    /**
     * 构建通话记录实体
     */
    private fun createCallEntity(protocol: ChatProtocol.CallMessage): MessageEntity {
        val now = System.currentTimeMillis()
        val contentType = if (protocol.callType.isVideoCall) {
            MessageType.VideoCall
        } else {
            MessageType.VoiceCall
        }

        return MessageEntity(
            messageId = protocol.messageId,
            sessionId = protocol.senderId,
            senderId = protocol.senderId,
            receiverId = protocol.receiverId,
            contentType = contentType,
            content = protocol.status,
            mediaDuration = protocol.duration,
            timestamp = protocol.timestamp,
            sendStatus = SendStatus.Delivered,
            isFromMe = false,
            createdAt = now,
            updatedAt = now
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
        val now = System.currentTimeMillis()
        val filename = if (protocol.messageType.isFileNameInJson) {
            json.decodeFromString<MediaContent>(protocol.content).filename
        } else {
            protocol.content
        }

        val localPath = fileManager.saveMediaFile(
            messageType = protocol.messageType,
            sourceFile = tempFile,
            messageId = protocol.messageId,
            extension = filename.extractFileExtension()
        ).also {
            tempFile.delete()
        }.getOrElse {
            Log.e(TAG, "保存媒体文件失败: ${protocol.messageId}", it)
            protocol.content
        }

        return MessageEntity(
            messageId = protocol.messageId,
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
            isFromMe = false,
            createdAt = now,
            updatedAt = now
        )
    }
}