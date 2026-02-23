package top.chengdongqing.wechat.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.SendError
import top.chengdongqing.wechat.data.database.entity.SendStatus

@Dao
interface MessageDao {

    @Query(
        """
        SELECT * FROM messages 
        WHERE sessionId = :sessionId 
        ORDER BY timestamp DESC 
        LIMIT :limit OFFSET :offset
    """
    )
    suspend fun getMessagesBySession(
        sessionId: String,
        limit: Int,
        offset: Int
    ): List<MessageEntity>

    @Query(
        """
        SELECT * FROM messages 
        WHERE sessionId = :sessionId 
        AND timestamp < :beforeTimestamp 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """
    )
    suspend fun getMessagesBeforeTimestamp(
        sessionId: String,
        beforeTimestamp: Long,
        limit: Int
    ): List<MessageEntity>

    @Query(
        """
        SELECT * FROM messages 
        WHERE sessionId = :sessionId 
        ORDER BY timestamp DESC
    """
    )
    fun observeMessagesBySession(sessionId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE messageId = :messageId")
    suspend fun getByMessageId(messageId: String): MessageEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE messageId = :messageId)")
    suspend fun exists(messageId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity): Long

    @Update
    suspend fun update(message: MessageEntity)

    @Query("UPDATE messages SET sentBytes = :sentBytes WHERE messageId = :messageId")
    suspend fun updateSentBytes(messageId: String, sentBytes: Long)

    @Query("UPDATE messages SET localPath = :localPath, updatedAt = :updatedAt WHERE messageId = :messageId")
    suspend fun updateLocalPath(
        messageId: String,
        localPath: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE messages SET sendStatus = :status WHERE messageId = :messageId")
    suspend fun updateSendStatus(messageId: String, status: SendStatus)

    @Query("UPDATE messages SET sendStatus = :status, failReason = :reason WHERE messageId = :messageId")
    suspend fun updateSendStatusAndFailReason(
        messageId: String,
        status: SendStatus,
        reason: SendError
    )

    @Query("UPDATE messages SET isRead = 1 WHERE sessionId = :sessionId AND isFromMe = 0")
    suspend fun markAllAsRead(sessionId: String)

    @Query("UPDATE messages SET isPlayed = 1 WHERE messageId = :messageId")
    suspend fun markAsPlayed(messageId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId AND isFromMe = 0 AND isRead = 0")
    suspend fun getUnreadCount(sessionId: String): Int

    @Delete
    suspend fun delete(message: MessageEntity)

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteByMessageId(messageId: String)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
