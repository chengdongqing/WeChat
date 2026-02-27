package top.chengdongqing.wechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.data.database.entity.MessageType

@Dao
interface ChatSessionDao : BaseDao<ChatSessionEntity> {

    @Query("SELECT * FROM chat_sessions WHERE isHidden = 0 ORDER BY isPinned DESC, lastMessageTime DESC")
    fun observeAll(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    fun observeById(sessionId: String): Flow<ChatSessionEntity?>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getById(sessionId: String): ChatSessionEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM chat_sessions WHERE id = :sessionId)")
    suspend fun exists(sessionId: String): Boolean

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
        SET lastMessage = :lastMessage,
            lastMessageType = :lastMessageType,
            lastMessageTime = :lastMessageTime,
            isSending = :isSending,
            updatedAt = :now,
            -- 如果隐藏了，则取消隐藏
            isHidden = CASE WHEN isHidden = 1 THEN 0 ELSE isHidden END
        WHERE id = :sessionId
        """
    )
    suspend fun updateLastMessage(
        sessionId: String,
        lastMessage: String,
        lastMessageType: MessageType?,
        lastMessageTime: Long,
        isSending: Boolean,
        now: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE chat_sessions 
        SET lastMessage = NULL, 
            unreadCount = 0,
            updatedAt = :now 
        WHERE id = :sessionId
    """
    )
    suspend fun clearLastMessage(sessionId: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: String)
}