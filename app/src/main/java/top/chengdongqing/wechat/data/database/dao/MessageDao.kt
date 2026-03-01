package top.chengdongqing.wechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.database.entity.MessageEntity

@Dao
interface MessageDao : BaseDao<MessageEntity> {

    @Query(
        """
        SELECT * FROM messages 
        WHERE sessionId = :sessionId 
        ORDER BY timestamp DESC
        LIMIT :limit
    """
    )
    fun observeBySessionId(sessionId: String, limit: Int): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getById(messageId: String): MessageEntity?

    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId AND isFromMe = 0 AND isRead = 0")
    suspend fun getUnreadCountBySessionId(sessionId: String): Int

    @Query(
        """
        SELECT DISTINCT localPath FROM messages 
        WHERE sessionId = :sessionId 
        AND localPath IS NOT NULL 
        AND localPath != ''
    """
    )
    suspend fun getLocalPathsBySessionId(sessionId: String): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE id = :messageId)")
    suspend fun exists(messageId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE sessionId = :sessionId AND timestamp < :lastTimestamp LIMIT 1)")
    suspend fun hasOlderMessages(sessionId: String, lastTimestamp: Long): Boolean

    @Transaction
    suspend fun update(messageId: String, updateBlock: (MessageEntity) -> MessageEntity) {
        val old = getById(messageId) ?: return
        val new = updateBlock(old).copy(
            audit = old.audit.copy(updatedAt = System.currentTimeMillis())
        )
        update(new)
    }

    @Query("UPDATE messages SET isRead = 1 WHERE sessionId = :sessionId AND isFromMe = 0")
    suspend fun markAsReadBySessionId(sessionId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: String)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)
}
