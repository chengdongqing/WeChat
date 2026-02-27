package top.chengdongqing.wechat.features.chat.data.repository

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.MessageType
import top.chengdongqing.wechat.data.database.entity.SendStatus
import top.chengdongqing.wechat.data.network.messaging.ChatSessionUpdater
import top.chengdongqing.wechat.data.network.messaging.MessageSender
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
    private val json: Json
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
    ): Result<Unit> {
        return runCatching {
            val myProfile = profileRepository.getCurrentProfileSnapshot()
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

            weDatabase.withTransaction {
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
                chatSessionUpdater.update(entity, !shouldSkipSend)
            }

            if (!shouldSkipSend) {
                // 发送消息
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

    override suspend fun deleteMessage(messageId: String) {
        messageDao.deleteById(messageId)
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
            Log.e(TAG, "发送失败: ${entity.id}", e)
        }
    }
}