package top.chengdongqing.wechat.features.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent

interface MessageRepository {

    /** 分页加载消息 */
    suspend fun getMessages(
        sessionId: String,
        limit: Int,
        beforeTimestamp: Long? = null
    ): List<ChatMessage>

    /** 监听消息 */
    fun observeMessages(sessionId: String): Flow<List<ChatMessage>>

    /** 发送消息 */
    suspend fun sendMessage(
        sessionId: String,
        receiverId: String,
        content: MessageContent
    ): Result<ChatMessage>

    /** 重试发送 */
    suspend fun retrySend(messageId: String): Result<Unit>

    /** 标记已读 */
    suspend fun markAllAsRead(sessionId: String)

    /** 标记已播放 */
    suspend fun markVoiceAsPlayed(messageId: String)

    /** 删除消息 */
    suspend fun deleteMessage(messageId: String)

    /** 删除会话所有消息 */
    suspend fun deleteSessionMessages(sessionId: String)
}