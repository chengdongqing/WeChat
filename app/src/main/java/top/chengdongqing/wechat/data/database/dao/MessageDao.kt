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

    @Query("SELECT * FROM messages WHERE id IN (:ids) ORDER BY timestamp ASC")
    suspend fun getByIds(ids: Set<String>): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId AND isFromMe = 0 AND isRead = 0")
    suspend fun getUnreadCountBySessionId(sessionId: String): Int

    @Query(
        """
        SELECT DISTINCT localPath FROM messages 
        WHERE sessionId = :sessionId 
        AND localPath IS NOT NULL 
    """
    )
    suspend fun getLocalPathsBySessionId(sessionId: String): List<String>

    @Query(
        """
      SELECT DISTINCT localPath FROM messages 
      WHERE id IN (:ids) 
      AND localPath IS NOT NULL
    """
    )
    suspend fun getLocalPathsByIds(ids: Set<String>): List<String>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getLatestMessage(sessionId: String): MessageEntity?

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

    @Query("UPDATE messages SET isRecalled = 1, updatedAt = :now WHERE id = :messageId")
    suspend fun markAsRecalledById(messageId: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE messages SET isRead = 1, updatedAt = :now WHERE sessionId = :sessionId AND isRead = 0 AND isFromMe = 0")
    suspend fun markAsReadBySessionId(sessionId: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: String)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)

    @Query("DELETE FROM messages WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: Set<String>)
}
