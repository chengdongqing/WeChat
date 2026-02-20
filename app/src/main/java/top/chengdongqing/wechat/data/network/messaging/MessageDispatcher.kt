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
import top.chengdongqing.wechat.data.database.entity.SendStatus
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
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
 * 纯业务逻辑，不感知传输细节。
 * 媒体消息通过 [File]（临时文件）传入，由 [FileManager] 持久化。
 */
@Singleton
class MessageDispatcher @Inject constructor(
    private val messageDao: MessageDao,
    private val connectionInfoDao: ConnectionInfoDao,
    private val messageSender: MessageSender,
    private val chatSessionUpdater: ChatSessionUpdater,
    private val fileManager: FileManager,
    private val signalingManager: SignalingManager,
    private val json: Json
) {
    private companion object {
        const val TAG = "MessageDispatcher"
    }

    private val _incomingMessageFlow = MutableSharedFlow<ChatMessage>(
        replay = 0, extraBufferCapacity = 64
    )
    val incomingMessageFlow: SharedFlow<ChatMessage> = _incomingMessageFlow.asSharedFlow()
    // ==================== 分发入口 ====================

    suspend fun dispatch(protocol: ChatProtocol) {
        runCatching {
            when (protocol) {
                is ChatProtocol.TextMessage -> handleTextMessage(protocol)
                is ChatProtocol.CallMessage -> handleCallMessage(protocol)
                is ChatProtocol.MessageAck -> handleAck(protocol)
                is ChatProtocol.MessageRead -> handleReadReceipt(protocol)
                is ChatProtocol.Signaling -> handleSignaling(protocol)
                is ChatProtocol.Handshake -> handleHeartbeat(protocol)
                else -> {}
            }
        }.onFailure { Log.e(TAG, "分发失败: ${protocol::class.simpleName}", it) }
    }

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

    private suspend fun handleIncomingChat(
        protocol: ChatProtocol,
        entityBuilder: suspend () -> MessageEntity
    ) {
        try {
            // 已存在该消息直接发送送达回执
            if (messageDao.exists(protocol.messageId)) {
                messageSender.sendAck(protocol.messageId, protocol.senderId)
                return
            }

            // 保存消息记录到数据库
            val entity = entityBuilder()
            messageDao.insert(entity)
            // 更新会话
            chatSessionUpdater.update(entity)
            // 发送送达回执
            messageSender.sendAck(protocol.messageId, protocol.senderId)
            // 推到消息流供发送消息通知
            _incomingMessageFlow.emit(entity.toDomain(json))
        } catch (e: Exception) {
            Log.e(TAG, "处理消息失败: ${protocol.messageId}", e)
        }
    }

    // ==================== 回执 ====================

    private suspend fun handleAck(protocol: ChatProtocol.MessageAck) {
        runCatching { messageDao.updateSendStatus(protocol.messageId, SendStatus.Delivered) }
            .onFailure { Log.e(TAG, "ACK 更新失败: ${protocol.messageId}", it) }
    }

    private suspend fun handleReadReceipt(protocol: ChatProtocol.MessageRead) {
        runCatching { messageDao.updateSendStatus(protocol.messageId, SendStatus.Read) }
            .onFailure { Log.e(TAG, "已读更新失败: ${protocol.messageId}", it) }
    }

    private suspend fun handleSignaling(protocol: ChatProtocol.Signaling) {
        signalingManager.onSignalingReceived(protocol)
    }

    private suspend fun handleHeartbeat(protocol: ChatProtocol.Handshake) {
        runCatching { connectionInfoDao.markOnline(protocol.senderId, protocol.timestamp) }
            .onFailure { Log.e(TAG, "心跳处理失败: ${protocol.senderId}", it) }
    }

    // ==================== 实体构建 ====================

    private fun createTextEntity(protocol: ChatProtocol.TextMessage): MessageEntity {
        val now = System.currentTimeMillis()
        return MessageEntity(
            messageId = protocol.messageId,
            sessionId = protocol.senderId,
            senderId = protocol.senderId,
            receiverId = protocol.receiverId,
            contentType = MessageType.Text,
            content = protocol.content,
            timestamp = protocol.timestamp,
            sendStatus = SendStatus.Delivered,
            isFromMe = false,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun createCallEntity(protocol: ChatProtocol.CallMessage): MessageEntity {
        val now = System.currentTimeMillis()
        return MessageEntity(
            messageId = protocol.messageId,
            sessionId = protocol.senderId,
            senderId = protocol.senderId,
            receiverId = protocol.receiverId,
            contentType = if (protocol.callType.isVideoCall) MessageType.VideoCall else MessageType.VoiceCall,
            content = protocol.status,
            mediaDuration = protocol.duration,
            timestamp = protocol.timestamp,
            sendStatus = SendStatus.Delivered,
            isFromMe = false,
            createdAt = now,
            updatedAt = now
        )
    }

    private suspend fun createMediaEntity(
        protocol: ChatProtocol.MediaMessage,
        tempFile: File
    ): MessageEntity {
        val now = System.currentTimeMillis()
        val filename = if (protocol.messageType.isFileNameInJson)
            json.decodeFromString<MediaContent>(protocol.content).filename
        else protocol.content
        val extension = filename.extractFileExtension()

        val localPath = fileManager.saveMediaFile(
            messageType = protocol.messageType,
            sourceFile = tempFile,
            messageId = protocol.messageId,
            extension = extension
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