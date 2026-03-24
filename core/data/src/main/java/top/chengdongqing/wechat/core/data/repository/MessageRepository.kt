package top.chengdongqing.wechat.core.data.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.ChatProtocol
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.database.entity.MessageEntity
import top.chengdongqing.wechat.core.model.SendError
import top.chengdongqing.wechat.core.model.SendStatus

interface MessageRepository {
    fun observeMessages(sessionId: String, limit: Int): Flow<List<ChatMessage>>
    suspend fun hasOlderMessages(sessionId: String, lastTimestamp: Long): Boolean
    suspend fun getMessage(messageId: String): ChatMessage?
    suspend fun sendMessage(
        sessionId: String,
        receiverId: String,
        messageId: String? = null,
        content: MessageContent
    ): Result<Unit>
    suspend fun retrySend(messageId: String): Result<Unit>
    suspend fun pauseTransfer(messageId: String): Result<Unit>
    suspend fun resumeTransfer(messageId: String): Result<Unit>
    suspend fun cancelTransfer(messageId: String): Result<Unit>
    suspend fun markAllAsRead(sessionId: String)
    suspend fun markVoiceAsPlayed(messageId: String)
    suspend fun deleteMessage(messageId: String)
    suspend fun recallMessage(messageId: String): Result<Unit>
    suspend fun deleteMessages(ids: Set<String>, sessionId: String)
    suspend fun forwardMessages(ids: Set<String>, targetChatIds: Set<String>)
    suspend fun handleIncomingMessage(
        protocol: ChatProtocol,
        entityBuilder: suspend () -> MessageEntity,
        onNotifyRequired: suspend (ChatMessage) -> Unit
    )
    suspend fun updateMessageStatus(
        messageId: String,
        status: SendStatus,
        failedReason: SendError? = null
    )
}
