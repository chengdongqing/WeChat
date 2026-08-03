package top.chengdongqing.wechat.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.core.model.MessageType

@Dao
interface ChatSessionDao : BaseDao<ChatSessionEntity> {

    @Query("SELECT * FROM chat_sessions WHERE isHidden = 0 ORDER BY isPinned DESC, lastMessageTime DESC")
    fun observeAll(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    fun observeById(sessionId: String): Flow<ChatSessionEntity?>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getById(sessionId: String): ChatSessionEntity?

    @Query(
        "SELECT id FROM chat_sessions " +
                "WHERE isTemporary = 1 AND expiresAt IS NOT NULL AND expiresAt <= :now"
    )
    suspend fun getExpiredTemporarySessionIds(now: Long): List<String>

    @Query(
        "UPDATE chat_sessions SET expiresAt = :expiresAt, updatedAt = :now " +
                "WHERE id = :sessionId AND isTemporary = 1"
    )
    suspend fun refreshTemporaryExpiration(
        sessionId: String,
        expiresAt: Long,
        now: Long = System.currentTimeMillis()
    )

    @Transaction
    suspend fun update(sessionId: String, updateBlock: (ChatSessionEntity) -> ChatSessionEntity) {
        val old = getById(sessionId) ?: return
        val new = updateBlock(old).copy(
            audit = old.audit.copy(updatedAt = System.currentTimeMillis())
        )
        update(new)
    }

    @Query("UPDATE chat_sessions SET unreadCount = unreadCount + 1, updatedAt = :now WHERE id = :sessionId")
    suspend fun incrementUnreadCount(sessionId: String, now: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE chat_sessions 
        SET lastMessageId = :lastMessageId,
            lastMessage = :lastMessage,
            lastMessageType = :lastMessageType,
            lastMessageTime = :lastMessageTime,
            lastMessageRecalled = :lastMessageRecalled,
            lastMessageFromMe = :lastMessageFromMe,
            isSending = :isSending,
            updatedAt = :now,
            -- 如果隐藏了，则取消隐藏
            isHidden = CASE WHEN isHidden = 1 THEN 0 ELSE isHidden END
        WHERE id = :sessionId
        """
    )
    suspend fun updateLastMessage(
        sessionId: String,
        lastMessageId: String,
        lastMessage: String,
        lastMessageType: MessageType?,
        lastMessageTime: Long,
        lastMessageRecalled: Boolean,
        lastMessageFromMe: Boolean,
        isSending: Boolean,
        now: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE chat_sessions 
        SET lastMessageRecalled = 1,
            updatedAt = :now,
            -- 如果隐藏了，则取消隐藏
            isHidden = CASE WHEN isHidden = 1 THEN 0 ELSE isHidden END
        WHERE id = :sessionId
            -- 以messageId为前提更新，避免并发时出现不一致
            AND lastMessageId = :messageId
        """
    )
    suspend fun markAsRecalled(
        sessionId: String,
        messageId: String,
        now: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE chat_sessions 
        SET lastMessage = NULL,
            lastMessageId = NULL,
            lastMessageType = NULL,
            isSending = 0,
            unreadCount = 0,
            updatedAt = :now 
        WHERE id = :sessionId
    """
    )
    suspend fun clearLastMessage(sessionId: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE chat_sessions SET isSending = 0 WHERE isSending = 1")
    suspend fun resetAllSendingSessions()

    @Query(
        """
        UPDATE chat_sessions 
        SET lastMessage = NULL,
            lastMessageId = NULL,
            isHidden = 1,
            unreadCount = 0,
            updatedAt = :now
    """
    )
    suspend fun clearAll(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: String)
}
