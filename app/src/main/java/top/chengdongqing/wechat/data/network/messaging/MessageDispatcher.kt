package top.chengdongqing.wechat.data.network.messaging

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.MessageType
import top.chengdongqing.wechat.data.database.entity.SendStatus
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.features.chat.data.mapper.toDomain
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息分发器
 */
@Singleton
class MessageDispatcher @Inject constructor(
    private val messageDao: MessageDao,
    private val connectionInfoDao: ConnectionInfoDao,
    private val messageSender: MessageSender,
    private val chatSessionUpdater: ChatSessionUpdater,
    private val json: Json
) {
    private companion object {
        const val TAG = "MessageDispatcher"
    }

    // ✅ 聊天消息（已存库）
    private val _incomingMessageFlow = MutableSharedFlow<ChatMessage>(
        replay = 0,
        extraBufferCapacity = 64  // 防止背压丢消息
    )
    val incomingMessageFlow: SharedFlow<ChatMessage> = _incomingMessageFlow.asSharedFlow()

    // ✅ 信令消息（不存库）
    private val _signalingFlow = MutableSharedFlow<SignalingMessage>(
        replay = 0,
        extraBufferCapacity = 32
    )
    val signalingFlow: SharedFlow<SignalingMessage> = _signalingFlow.asSharedFlow()

    // ==================== 分发入口 ====================

    suspend fun dispatch(protocol: ChatProtocol) {
        when (protocol) {
            is ChatProtocol.TextMessage -> handleChatMessage(protocol)
            is ChatProtocol.MediaMessage -> handleChatMessage(protocol)
            is ChatProtocol.MessageAck -> handleAck(protocol)
            is ChatProtocol.MessageRead -> handleRead(protocol)
            is ChatProtocol.Signaling -> handleSignaling(protocol)
            is ChatProtocol.Heartbeat -> handleHeartbeat(protocol)
        }
    }

    // ==================== 聊天消息 ====================

    private suspend fun handleChatMessage(protocol: ChatProtocol) {
        try {
            // 去重
            if (messageDao.getByMessageId(protocol.messageId) != null) {
                Log.d(TAG, "消息已存在，跳过: ${protocol.messageId}")
                messageSender.sendAck(protocol.messageId, protocol.senderId)
                return
            }

            // 解密（预留扩展点）
            val decrypted = decrypt(protocol)

            // 持久化
            val entity = buildEntity(decrypted)
            messageDao.insert(entity)

            // 更新会话
            chatSessionUpdater.update(entity)

            // 回复 ACK
            messageSender.sendAck(protocol.messageId, protocol.senderId)

            // 推入 MessageFlow
            _incomingMessageFlow.emit(entity.toDomain(json))

            Log.d(TAG, "✅ 消息已处理: ${protocol.messageId}")
        } catch (e: Exception) {
            Log.e(TAG, "处理聊天消息失败", e)
        }
    }

    // ==================== 回执 ====================

    private suspend fun handleAck(protocol: ChatProtocol.MessageAck) {
        messageDao.updateSendStatus(protocol.messageId, SendStatus.Delivered)
        Log.d(TAG, "消息已送达: ${protocol.messageId}")
    }

    private suspend fun handleRead(protocol: ChatProtocol.MessageRead) {
        messageDao.updateSendStatus(protocol.messageId, SendStatus.Read)
        Log.d(TAG, "消息已读: ${protocol.messageId}")
    }

    // ==================== 信令 ====================

    private suspend fun handleSignaling(protocol: ChatProtocol.Signaling) {

    }

    // ==================== 心跳 ====================

    private suspend fun handleHeartbeat(protocol: ChatProtocol.Heartbeat) {
        // 更新对方在线时间
        connectionInfoDao.markOnline(protocol.senderId, protocol.timestamp)
    }

    // ==================== 工具方法 ====================

    /**
     * 统一解密入口
     */
    private fun decrypt(protocol: ChatProtocol): ChatProtocol {
        // TODO
        return protocol
    }

    private fun buildEntity(protocol: ChatProtocol): MessageEntity {
        val now = System.currentTimeMillis()
        return when (protocol) {
            is ChatProtocol.TextMessage -> MessageEntity(
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

            is ChatProtocol.MediaMessage -> MessageEntity(
                messageId = protocol.messageId,
                sessionId = protocol.senderId,
                senderId = protocol.senderId,
                receiverId = protocol.receiverId,
                contentType = protocol.messageType,
                content = protocol.content,
                mediaSize = protocol.mediaSize,
                timestamp = protocol.timestamp,
                sendStatus = SendStatus.Delivered,
                isFromMe = false,
                createdAt = now,
                updatedAt = now
            )

            else -> throw IllegalArgumentException("不支持的消息类型: ${protocol::class.simpleName}")
        }
    }
}