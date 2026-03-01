package top.chengdongqing.wechat.features.chat.data.repository

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.MessageType
import top.chengdongqing.wechat.data.database.entity.SendStatus
import top.chengdongqing.wechat.data.network.messaging.ChatSessionUpdater
import top.chengdongqing.wechat.data.network.messaging.MessageSender
import top.chengdongqing.wechat.data.network.transfer.TransferManager
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
    private val weDatabase: WeDatabase,
    private val messageDao: MessageDao,
    private val chatSessionRepository: ChatSessionRepository,
    private val messageSender: MessageSender,
    private val profileRepository: ProfileRepository,
    private val chatSessionUpdater: ChatSessionUpdater,
    private val transferManager: TransferManager,
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

    override suspend fun sendMessage(
        sessionId: String,
        receiverId: String,
        messageId: String?,
        content: MessageContent
    ): Result<Unit> = runCatching {
        val myProfile = profileRepository.getCurrentProfileSnapshot()
            ?: throw Exception("未找到个人资料")
        val finalMessageId = messageId ?: randomUUID()
        val isSelf = receiverId == myProfile.id
        val isCall = content is MessageContent.Call
        val shouldSkipSend = isSelf || isCall // 如果是给自己发的，或者是通话记录，直接设置为发送成功，且不走发送逻辑

        // 构建消息实体
        val entity = content.toEntity(
            messageId = finalMessageId,
            sessionId = sessionId,
            senderId = myProfile.id,
            receiverId = receiverId,
            timestamp = System.currentTimeMillis(),
            json = json
        ).copy(
            // 如果需要发送，初始状态设为 Sending，否则直接 Sent
            sendStatus = if (shouldSkipSend) SendStatus.Sent else SendStatus.Sending
        )

        weDatabase.withTransaction {
            // 保存到数据库
            messageDao.insert(entity)
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
                MessageType.ContactCard -> messageSender.sendTextMessage(entity)

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
        weDatabase.withTransaction {
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
        messageDao.getById(messageId).let {
            // 删除可能存在的媒体文件
            try {
                it?.localPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e("DeleteMessage", "Error deleting file of message $messageId", e)
            }

            // 删除消息
            messageDao.deleteById(messageId)
        }
    }

    override suspend fun deleteSessionMessages(sessionId: String) {
        messageDao.deleteBySessionId(sessionId)
    }

    /**
     * 异步发送消息
     */
    private suspend fun sendMessageAsync(entity: MessageEntity) {
        try {
            when (entity.contentType) {
                MessageType.Text,
                MessageType.ContactCard -> messageSender.sendTextMessage(entity)

                else -> {
                    val file = File(entity.localPath ?: throw Exception("文件路径为空"))
                    messageSender.sendMediaMessage(entity, file)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "发送失败: ${entity.id}, ${e.message}")
        }
    }
}