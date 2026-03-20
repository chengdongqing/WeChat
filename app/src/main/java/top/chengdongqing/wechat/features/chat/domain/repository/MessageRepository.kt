package top.chengdongqing.wechat.features.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.model.SendError
import top.chengdongqing.wechat.data.model.SendStatus
import top.chengdongqing.wechat.data.network.model.ChatProtocol
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent

interface MessageRepository {

    /**
     * 监听消息
     */
    fun observeMessages(sessionId: String, limit: Int): Flow<List<ChatMessage>>

    /**
     * 查询是否有更多数据
     */
    suspend fun hasOlderMessages(sessionId: String, lastTimestamp: Long): Boolean

    /**
     * 获取消息详情
     */
    suspend fun getMessage(messageId: String): ChatMessage?

    /**
     * 发送消息
     */
    suspend fun sendMessage(
        sessionId: String,
        receiverId: String,
        messageId: String? = null,
        content: MessageContent
    ): Result<Unit>

    /**
     * 重试发送
     */
    suspend fun retrySend(messageId: String): Result<Unit>

    /**
     * 暂停文件传输（发送或接收）
     */
    suspend fun pauseTransfer(messageId: String)

    /**
     * 恢复文件传输
     */
    suspend fun resumeTransfer(messageId: String)

    /**
     * 取消文件传输，清理相关资源
     */
    suspend fun cancelTransfer(messageId: String)

    /**
     * 标记已读
     */
    suspend fun markAllAsRead(sessionId: String)

    /**
     * 标记已播放
     */
    suspend fun markVoiceAsPlayed(messageId: String)

    /**
     * 删除消息
     */
    suspend fun deleteMessage(messageId: String)

    /**
     * 撤回消息
     */
    suspend fun recallMessage(messageId: String): Result<Unit>

    /**
     * 批量删除消息
     */
    suspend fun deleteMessages(ids: Set<String>, sessionId: String)

    /**
     * 批量转发消息
     */
    suspend fun forwardMessages(ids: Set<String>, targetChatIds: Set<String>)

    /**
     * 处理新消息
     */
    suspend fun handleIncomingMessage(
        protocol: ChatProtocol,
        entityBuilder: suspend () -> MessageEntity,
        onNotifyRequired: suspend (ChatMessage) -> Unit
    )

    /**
     * 更新消息状态
     */
    suspend fun updateMessageStatus(
        messageId: String,
        status: SendStatus,
        failedReason: SendError? = null
    )
}