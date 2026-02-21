package top.chengdongqing.wechat.features.chat.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.MessageType
import top.chengdongqing.wechat.data.database.entity.SendError
import top.chengdongqing.wechat.data.database.entity.SendStatus
import top.chengdongqing.wechat.data.network.messaging.ChatSessionUpdater
import top.chengdongqing.wechat.data.network.messaging.MessageSender
import top.chengdongqing.wechat.features.chat.data.mapper.toDomain
import top.chengdongqing.wechat.features.chat.data.mapper.toEntity
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.domain.repository.MessageRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val chatSessionDao: ChatSessionDao,
    private val messageSender: MessageSender,
    private val profileRepository: ProfileRepository,
    private val chatSessionUpdater: ChatSessionUpdater,
    private val json: Json
) : MessageRepository {

    companion object {
        private const val TAG = "MessageRepository"
    }

    override suspend fun getMessages(
        sessionId: String,
        limit: Int,
        beforeTimestamp: Long?
    ): List<ChatMessage> {
        val entities = if (beforeTimestamp != null) {
            messageDao.getMessagesBeforeTimestamp(sessionId, beforeTimestamp, limit)
        } else {
            messageDao.getMessagesBySession(sessionId, limit, 0)
        }
        return entities.map { it.toDomain(json) }
    }

    override fun observeMessages(sessionId: String): Flow<List<ChatMessage>> {
        return messageDao.observeMessagesBySession(sessionId).map { list ->
            list.map { it.toDomain(json) }
        }
    }

    override suspend fun sendMessage(
        sessionId: String,
        receiverId: String,
        messageId: String?,
        content: MessageContent
    ): Result<ChatMessage> {
        return runCatching {
            val myProfile = profileRepository.getCurrentProfileOnce()
                ?: throw Exception("未找到个人资料")
            val isSelfSession = receiverId == myProfile.id
            val isCallMessage = content is MessageContent.Call
            // 如果是给自己发的，或者是通话记录，直接设置为发送成功，且不走发送逻辑
            val shouldSkipSend = isSelfSession || isCallMessage

            // 构建消息实体
            val messageId = messageId ?: randomUUID()
            val now = System.currentTimeMillis()

            val entity = content.toEntity(
                messageId = messageId,
                sessionId = sessionId,
                senderId = myProfile.id,
                receiverId = receiverId,
                timestamp = now,
                json = json
            )
            // 保存到数据库
            messageDao.insert(
                entity.copy(
                    sendStatus = if (shouldSkipSend) {
                        SendStatus.Sent
                    } else {
                        entity.sendStatus
                    }
                )
            )

            // 更新会话
            chatSessionUpdater.update(entity)

            if (!shouldSkipSend) {
                // 发送消息
                sendMessageAsync(entity)
            }

            entity.toDomain(json)
        }
    }

    override suspend fun retrySend(messageId: String): Result<Unit> {
        return runCatching {
            val entity = messageDao.getByMessageId(messageId)
                ?: throw Exception("消息不存在")

            // 重置为发送中
            messageDao.updateSendStatus(messageId, SendStatus.Sending)

            // 重新发送
            when (entity.contentType) {
                MessageType.Text -> messageSender.sendTextMessage(entity).getOrThrow()
                else -> {
                    val file = File(entity.localPath ?: throw Exception("文件路径为空"))
                    messageSender.sendMediaMessage(entity, file)
                }
            }
        }
    }

    override suspend fun markAllAsRead(sessionId: String) {
        messageDao.markAllAsRead(sessionId)
        chatSessionDao.clearUnreadCount(sessionId)
    }

    override suspend fun markVoiceAsPlayed(messageId: String) {
        messageDao.markAsPlayed(messageId)
    }

    override suspend fun deleteMessage(messageId: String) {
        messageDao.deleteByMessageId(messageId)
    }

    override suspend fun deleteSessionMessages(sessionId: String) {
        messageDao.deleteBySession(sessionId)
    }

    // ==================== 私有方法 ====================

    /**
     * 异步发送消息，失败时更新状态
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
        } catch (e: CancellationException) {
            throw e  // 取消异常必须重新抛出
        } catch (e: Exception) {
            Log.e(TAG, "发送失败: ${entity.messageId}", e)

            val failReason = when {
                e.message?.contains("离线") == true -> SendError.RecipientOffline
                e.message?.contains("连接") == true -> SendError.NetworkTimeout
                else -> SendError.Unknown
            }

            messageDao.updateSendStatus(entity.messageId, SendStatus.Failed)
            messageDao.updateFailReason(entity.messageId, failReason)
        }
    }
}