package top.chengdongqing.wechat.features.chat.data.repository

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.core.util.deleteLocalFile
import top.chengdongqing.wechat.core.util.deleteLocalFiles
import top.chengdongqing.wechat.core.util.isWithinSeconds
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.model.MessageType
import top.chengdongqing.wechat.data.model.SendError
import top.chengdongqing.wechat.data.model.SendStatus
import top.chengdongqing.wechat.data.network.messaging.ChatSessionUpdater
import top.chengdongqing.wechat.data.network.messaging.MessageSender
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.ReceiptType
import top.chengdongqing.wechat.data.network.transfer.TransferManager
import top.chengdongqing.wechat.data.notification.NotificationHelper
import top.chengdongqing.wechat.data.session.ActiveSessionManager
import top.chengdongqing.wechat.data.session.FileReferenceManager
import top.chengdongqing.wechat.features.chat.data.mapper.toDomain
import top.chengdongqing.wechat.features.chat.data.mapper.toEntity
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import top.chengdongqing.wechat.features.chat.domain.repository.MessageRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import java.io.File
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val database: WeDatabase,
    private val messageDao: MessageDao,
    private val chatSessionDao: ChatSessionDao,
    private val chatSessionRepository: ChatSessionRepository,
    private val activeSessionManager: ActiveSessionManager,
    private val messageSender: MessageSender,
    private val profileRepository: ProfileRepository,
    private val chatSessionUpdater: ChatSessionUpdater,
    private val fileReferenceManager: FileReferenceManager,
    private val transferManager: TransferManager,
    private val notificationHelper: NotificationHelper,
    private val json: Json,
    @param:IoScope private val scope: CoroutineScope
) : MessageRepository {

    companion object {
        private const val TAG = "MessageRepository"
    }

    override fun observeMessages(sessionId: String, limit: Int): Flow<List<ChatMessage>> {
        return messageDao.observeBySessionId(sessionId, limit).map { list ->
            list.map { it.toDomain(json) }
        }
    }

    override suspend fun hasOlderMessages(
        sessionId: String,
        lastTimestamp: Long
    ): Boolean = messageDao.hasOlderMessages(sessionId, lastTimestamp)

    override suspend fun getMessage(messageId: String): ChatMessage? {
        return messageDao.getById(messageId)?.toDomain(json)
    }

    override suspend fun sendMessage(
        sessionId: String,
        receiverId: String,
        messageId: String?,
        content: MessageContent
    ): Result<Unit> = runCatching {
        val myProfile = profileRepository.getProfile()
            ?: throw Exception("未找到个人资料")
        val finalMessageId = messageId ?: randomUUID()
        val isSelf = receiverId == myProfile.id
        val isCall = content is MessageContent.Call
        val shouldSkipSend = isSelf || isCall // 如果是给自己发的，或者是通话记录，直接设置为发送成功，不走发送逻辑

        // 构建消息实体
        val entity = content.toEntity(
            messageId = finalMessageId,
            sessionId = sessionId,
            senderId = myProfile.id,
            receiverId = receiverId,
            timestamp = System.currentTimeMillis(),
            json = json
        ).copy(
            // 如果需要发送，初始状态设为 Sending，否则直接 Delivered
            sendStatus = if (shouldSkipSend) SendStatus.Delivered else SendStatus.Sending
        )

        database.withTransaction {
            // 保存到数据库
            messageDao.insert(entity)
            // 注册文件引用
            fileReferenceManager.retain(entity.localPath)
            // 更新会话
            chatSessionUpdater.update(entity, !shouldSkipSend)
        }

        if (!shouldSkipSend) {
            // 切入后台作用域执行网络发送
            scope.launch {
                sendMessageAsync(entity)
            }
        }
    }

    /**
     * 异步发送消息
     */
    private suspend fun sendMessageAsync(entity: MessageEntity) {
        try {
            when (entity.contentType) {
                MessageType.Text,
                MessageType.ContactCard,
                MessageType.Music -> messageSender.sendTextMessage(entity)

                else -> {
                    val file = File(entity.localPath ?: throw Exception("文件路径为空"))
                    messageSender.sendMediaMessage(entity, file)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "发送失败: ${entity.id}, ${e.message}")
        }
    }

    override suspend fun retrySend(messageId: String): Result<Unit> {
        return runCatching {
            val entity = messageDao.getById(messageId)
                ?: throw Exception("消息不存在")

            // 重置为发送中
            messageDao.update(messageId) { session ->
                session.copy(sendStatus = SendStatus.Sending)
            }

            // 重新发送
            when (entity.contentType) {
                MessageType.Text,
                MessageType.ContactCard,
                MessageType.Music -> messageSender.sendTextMessage(entity)

                else -> {
                    val file = File(entity.localPath ?: throw Exception("文件路径为空"))
                    messageSender.sendMediaMessage(entity, file)
                }
            }
        }
    }

    override fun stopTransfer(messageId: String) {
        transferManager.setCancelled(messageId)
    }

    override suspend fun markAllAsRead(sessionId: String) {
        database.withTransaction {
            messageDao.markAsReadBySessionId(sessionId)
            chatSessionRepository.clearUnreadCount(sessionId)
        }
    }

    override suspend fun markVoiceAsPlayed(messageId: String) {
        messageDao.update(messageId) { message ->
            message.copy(isPlayed = true)
        }
    }

    override suspend fun deleteMessage(messageId: String) = withContext(Dispatchers.IO) {
        // 查询消息详情
        val message = messageDao.getById(messageId)

        database.withTransaction {
            // 删除消息
            messageDao.deleteById(messageId)

            message?.let {
                // 重新设置会话
                updateLastMessage(it.sessionId)
            }
        }

        // 删除关联的媒体文件
        message?.localPath?.let { path ->
            val toDelete = fileReferenceManager.release(path)
            toDelete?.let { deleteLocalFile(it) }
        }

        Unit
    }

    override suspend fun recallMessage(messageId: String): Result<Unit> = runCatching {
        val message = messageDao.getById(messageId) ?: throw IllegalStateException("消息不存在")
        val isFromMe = message.isFromMe

        // 判断是否是5分钟内发送的消息
        if (!message.timestamp.isWithinSeconds()) {
            throw IllegalStateException("只能撤回5分钟内发送的消息")
        }

        // 标记为已撤回
        database.withTransaction {
            // 更新消息
            messageDao.update(messageId) { message ->
                message.copy(
                    isRecalled = true,
                    content = if (isFromMe) message.content else "" // 对方撤回的置空消息内容
                )
            }
            // 更新会话
            chatSessionDao.markAsRecalledByMessageId(
                message.sessionId,
                messageId,
                if (isFromMe) "你撤回了一条消息" else "对方撤回了一条消息"
            )
        }

        // 清除通知
        if (!isFromMe) {
            notificationHelper.cancelNotification(message.sessionId.hashCode())
        }

        // 删除可能存在的媒体文件
        message.localPath?.let { path ->
            val toDelete = fileReferenceManager.release(path)
            toDelete?.let { deleteLocalFile(it) }
        }

        // 给对方发送撤回申请
        if (isFromMe) {
            scope.launch {
                messageSender.sendReceipt(
                    messageId = messageId,
                    receiverId = message.receiverId,
                    type = ReceiptType.Recalled
                )
            }
        }
    }

    override suspend fun deleteMessages(ids: Set<String>, sessionId: String) =
        withContext(Dispatchers.IO) {
            // 查询消息关联的媒体文件
            val localPaths = messageDao.getLocalPathsByIds(ids)

            database.withTransaction {
                // 批量删除消息记录
                messageDao.deleteByIds(ids)

                // 重新设置会话
                updateLastMessage(sessionId)
            }

            // 批量删除可能存在的本地文件
            val toDelete = fileReferenceManager.releaseAll(localPaths)
            deleteLocalFiles(toDelete)

            Unit
        }

    override suspend fun forwardMessages(
        ids: Set<String>,
        targetChatIds: Set<String>
    ) = withContext(Dispatchers.IO) {
        // 查询原消息
        val messages = messageDao.getByIds(ids).filter {
            // 过滤不可转发的消息
            it.contentType.isForwardable
        }

        // 为每个目标会话并行转发
        targetChatIds.map { targetChatId ->
            async {
                messages.forEach { entity ->
                    val content = entity.toDomain(json).content
                    sendMessage(
                        sessionId = targetChatId,
                        receiverId = targetChatId,
                        content = content
                    )
                }
            }
        }.awaitAll()

        Unit
    }

    private suspend fun updateLastMessage(sessionId: String) {
        // 查询最新的消息
        val latestMessage = messageDao.getLatestMessage(sessionId)

        if (latestMessage != null) {
            // 更新到session
            chatSessionUpdater.update(
                message = latestMessage,
                isSending = latestMessage.sendStatus == SendStatus.Sending
            )
        } else {
            // 直接清除
            chatSessionDao.clearLastMessage(sessionId)
        }
    }

    override suspend fun handleIncomingMessage(
        protocol: ChatProtocol,
        entityBuilder: suspend () -> MessageEntity,
        onNotifyRequired: suspend (ChatMessage) -> Unit
    ) {
        try {
            // 已存在该消息直接发送送达回执
            if (messageDao.exists(protocol.messageId)) {
                sendAck(protocol)
                return
            }

            val entity = entityBuilder()
            database.withTransaction {
                // 保存消息
                messageDao.insert(entity)
                // 更新会话
                chatSessionUpdater.update(entity)
            }
            // 发送送达回执
            sendAck(protocol)

            // 查询是否开启免打扰
            val isMuted = chatSessionRepository.isSessionMuted(protocol.senderId)

            // 满足指定条件就推送到消息流以触发消息通知
            val isNotifyRequired = !isMuted && // 未开启免打扰
                    !entity.isFromMe && // 不是给自己发的
                    !entity.contentType.isCallMessage && // 不是通话消息
                    !activeSessionManager.isActive(entity.sessionId) // 当前未在消息所在的会话页
            if (isNotifyRequired) {
                onNotifyRequired(entity.toDomain(json))
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理消息失败: ${protocol.messageId}", e)
        }
    }

    /**
     * 发送送达回执
     */
    private fun sendAck(protocol: ChatProtocol) {
        scope.launch {
            messageSender.sendReceipt(
                messageId = protocol.messageId,
                receiverId = protocol.senderId,
                type = ReceiptType.Delivered
            )
        }
    }

    override suspend fun updateMessageStatus(
        messageId: String,
        status: SendStatus,
        failedReason: SendError?
    ) {
        messageDao.update(messageId) { message ->
            message.copy(
                sendStatus = status,
                failReason = failedReason
            )
        }

        if (failedReason != null) {
            // 停止发送文件（如果有）
            transferManager.setCancelled(messageId)
        }
    }
}