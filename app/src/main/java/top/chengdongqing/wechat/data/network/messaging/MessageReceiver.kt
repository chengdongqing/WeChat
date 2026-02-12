package top.chengdongqing.wechat.data.network.messaging

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.MessageType
import top.chengdongqing.wechat.data.database.entity.SendStatus
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.socket.SocketConnection
import top.chengdongqing.wechat.data.network.socket.SocketServer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息接收器
 */
@Singleton
class MessageReceiver @Inject constructor(
    private val socketServer: SocketServer,
    private val messageDao: MessageDao,
    private val chatSessionDao: ChatSessionDao,
    private val messageSender: MessageSender,
    private val json: Json
) {

    private companion object {
        const val TAG = "MessageReceiver"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _newMessages = MutableSharedFlow<MessageEntity>()
    val newMessages: SharedFlow<MessageEntity> = _newMessages.asSharedFlow()

    /**
     * 启动接收服务
     */
    fun start() {
        // 监听新连接
        scope.launch {
            socketServer.incomingConnections.collect { incoming ->
                handleIncomingConnection(incoming.connection)
            }
        }

        Log.d(TAG, "消息接收服务已启动")
    }

    /**
     * 开始监听某个连接的消息
     */
    fun startListening(connection: SocketConnection) {
        scope.launch {
            for (data in connection.receiveChannel) {
                handleReceivedData(data, connection.userId)
            }
        }
    }

    /**
     * 处理新连接
     */
    private fun handleIncomingConnection(connection: top.chengdongqing.wechat.data.network.socket.ClientConnection) {
        scope.launch {
            for (data in connection.receiveChannel) {
                handleReceivedData(data, connection.userId)
            }
        }
    }

    /**
     * 处理接收到的数据
     */
    private suspend fun handleReceivedData(data: ByteArray, senderId: String) {
        try {
            val jsonString = String(data, Charsets.UTF_8)

            // 解析协议
            when (val protocol = json.decodeFromString<ChatProtocol>(jsonString)) {
                is ChatProtocol.TextMessage -> handleTextMessage(protocol)
                is ChatProtocol.MediaMessage -> handleMediaMessage(protocol)
                is ChatProtocol.MessageAck -> handleMessageAck(protocol)
                is ChatProtocol.MessageRead -> handleMessageRead(protocol)
                is ChatProtocol.OnlineStatus -> handleOnlineStatus(protocol)
                is ChatProtocol.Heartbeat -> handleHeartbeat(protocol)
            }

        } catch (e: Exception) {
            Log.e(TAG, "处理消息失败", e)
        }
    }

    /**
     * 处理文本消息
     */
    private suspend fun handleTextMessage(protocol: ChatProtocol.TextMessage) {
        Log.d(TAG, "收到文本消息: ${protocol.messageId}")

        // 1. 保存到数据库
        val message = MessageEntity(
            messageId = protocol.messageId,
            sessionId = protocol.senderId,
            senderId = protocol.senderId,
            receiverId = protocol.receiverId,
            contentType = MessageType.Text,
            content = protocol.content,
            timestamp = protocol.timestamp,
            sendStatus = SendStatus.Delivered,
            isFromMe = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        messageDao.insert(message)

        // 2. 更新会话
        updateChatSession(protocol.senderId, protocol.content, protocol.timestamp)

        // 3. 发送 ACK
        messageSender.sendAck(protocol.messageId, protocol.senderId)

        // 4. 通知UI
        _newMessages.emit(message)
    }

    /**
     * 处理媒体消息
     */
    private suspend fun handleMediaMessage(protocol: ChatProtocol.MediaMessage) {
        Log.d(TAG, "收到媒体消息: ${protocol.messageId}")

        // 保存消息元数据（文件数据会在后续接收）
        val message = MessageEntity(
            messageId = protocol.messageId,
            sessionId = protocol.senderId,
            senderId = protocol.senderId,
            receiverId = protocol.receiverId,
            contentType = protocol.messageType,
            content = protocol.content,
            mediaSize = protocol.mediaSize,
            mediaDuration = protocol.mediaDuration,
            timestamp = protocol.timestamp,
            sendStatus = SendStatus.Delivered,
            isFromMe = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        messageDao.insert(message)

        // 更新会话
        val contentText = when (protocol.messageType) {
            MessageType.Image -> "[图片]"
            MessageType.VoiceCall -> "[语音]"
            MessageType.Video -> "[视频]"
            MessageType.File -> "[文件]"
            else -> "[消息]"
        }
        updateChatSession(protocol.senderId, contentText, protocol.timestamp)

        // 发送 ACK
        messageSender.sendAck(protocol.messageId, protocol.senderId)

        // 通知UI
        _newMessages.emit(message)
    }

    /**
     * 处理消息确认
     */
    private suspend fun handleMessageAck(protocol: ChatProtocol.MessageAck) {
        Log.d(TAG, "收到ACK: ${protocol.messageId}")
        messageDao.updateSendStatus(protocol.messageId, SendStatus.Delivered)
    }

    /**
     * 处理已读回执
     */
    private suspend fun handleMessageRead(protocol: ChatProtocol.MessageRead) {
        Log.d(TAG, "收到已读回执: ${protocol.messageId}")
        messageDao.updateSendStatus(protocol.messageId, SendStatus.Read)
    }

    /**
     * 处理在线状态
     */
    private suspend fun handleOnlineStatus(protocol: ChatProtocol.OnlineStatus) {
        Log.d(TAG, "在线状态: ${protocol.userId} - ${protocol.isOnline}")
        // 更新连接信息表
    }

    /**
     * 处理心跳
     */
    private suspend fun handleHeartbeat(protocol: ChatProtocol.Heartbeat) {
        // 心跳保活，不需要特殊处理
    }

    /**
     * 更新聊天会话
     */
    private suspend fun updateChatSession(
        contactId: String,
        lastMessage: String,
        timestamp: Long
    ) {
        val existing = chatSessionDao.getById(contactId)

        if (existing != null) {
            // 更新现有会话
            val updated = existing.copy(
                lastMessage = lastMessage,
                lastMessageTime = timestamp,
                unreadCount = existing.unreadCount + 1,
                updatedAt = System.currentTimeMillis()
            )
            chatSessionDao.update(updated)
        } else {
            // 创建新会话（需要从联系人获取信息）
            val newSession = ChatSessionEntity(
                sessionId = contactId,
                contactId = contactId,
                contactName = "Unknown",  // 应该从联系人表查询
                contactAvatar = null,
                lastMessage = lastMessage,
                lastMessageType = MessageType.Text,
                lastMessageTime = timestamp,
                unreadCount = 1,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            chatSessionDao.insert(newSession)
        }
    }
}