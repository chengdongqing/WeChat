package top.chengdongqing.wechat.features.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent

interface MessageRepository {

    /** 监听消息 */
    fun observeMessages(sessionId: String, limit: Int): Flow<List<ChatMessage>>

    /** 查询是否有更多数据 */
    suspend fun hasOlderMessages(sessionId: String, lastTimestamp: Long): Boolean

    /** 发送消息 */
    suspend fun sendMessage(
        sessionId: String,
        receiverId: String,
        messageId: String? = null,
        content: MessageContent
    ): Result<Unit>

    /** 重试发送 */
    suspend fun retrySend(messageId: String): Result<Unit>

    /** 停止文件传输 */
    fun stopTransfer(messageId: String)

    /** 标记已读 */
    suspend fun markAllAsRead(sessionId: String)

    /** 标记已播放 */
    suspend fun markVoiceAsPlayed(messageId: String)

    /** 删除消息 */
    suspend fun deleteMessage(messageId: String)

    /** 撤回消息 */
    suspend fun recallMessage(messageId: String): Result<Unit>

    /** 批量删除消息 */
    suspend fun deleteMessages(ids: Set<String>, sessionId: String)

    /** 批量转发消息 */
    suspend fun forwardMessages(ids: Set<String>, targetChatIds: Set<String>)
}