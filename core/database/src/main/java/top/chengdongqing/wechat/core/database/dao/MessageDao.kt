package top.chengdongqing.wechat.core.database.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import top.chengdongqing.wechat.core.database.entity.MessageEntity
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.model.SendError
import top.chengdongqing.wechat.core.model.SendStatus

@Dao
interface MessageDao : BaseDao<MessageEntity> {

    @Query(
        """
        SELECT * FROM messages 
        WHERE sessionId = :sessionId 
        ORDER BY timestamp DESC
    """
    )
    fun pagingSource(sessionId: String): PagingSource<Int, MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE id IN (:ids) ORDER BY timestamp ASC")
    suspend fun getByIds(ids: Set<String>): List<MessageEntity>

    @Query(
        """SELECT * FROM messages
           WHERE isFromMe = 1 AND receiverId = :peerId
             AND sendStatus IN (:statuses)
           ORDER BY timestamp ASC
           LIMIT :limit"""
    )
    suspend fun getPendingOutgoing(
        peerId: String,
        statuses: Array<SendStatus> = arrayOf(
            SendStatus.Sending,
            SendStatus.Sent,
            SendStatus.Failed
        ),
        limit: Int = 100
    ): List<MessageEntity>

    @Query(
        """SELECT * FROM messages
           WHERE isFromMe = 1 AND attemptCount < :maxAttempts
             AND (
               (sendStatus = :sentStatus AND ackDeadlineAt IS NOT NULL AND ackDeadlineAt <= :now)
               OR
               (sendStatus = :failedStatus AND nextRetryAt IS NOT NULL AND nextRetryAt <= :now
                 AND failReason != :cancelledError)
             )
           ORDER BY COALESCE(nextRetryAt, ackDeadlineAt) ASC
           LIMIT :limit"""
    )
    suspend fun getDueOutgoing(
        now: Long,
        maxAttempts: Int,
        sentStatus: SendStatus = SendStatus.Sent,
        failedStatus: SendStatus = SendStatus.Failed,
        cancelledError: SendError = SendError.Cancelled,
        limit: Int = 50
    ): List<MessageEntity>

    @Query(
        """UPDATE messages
           SET sendStatus = :failedStatus, failReason = :failReason,
               ackDeadlineAt = NULL, nextRetryAt = NULL
           WHERE isFromMe = 1 AND sendStatus = :sentStatus
             AND ackDeadlineAt IS NOT NULL AND ackDeadlineAt <= :now
             AND attemptCount >= :maxAttempts"""
    )
    suspend fun failExhaustedAckWaits(
        now: Long,
        maxAttempts: Int,
        sentStatus: SendStatus = SendStatus.Sent,
        failedStatus: SendStatus = SendStatus.Failed,
        failReason: SendError = SendError.ConnectionFailed
    )

    @Query("UPDATE messages SET sendStatus = :targetStatus WHERE sendStatus in (:currentStatus) and sentBytes > 0")
    suspend fun pauseOngoingTransfers(
        targetStatus: SendStatus = SendStatus.Paused,
        currentStatus: Array<SendStatus> = arrayOf(SendStatus.Sending, SendStatus.Receiving)
    )

    @Query("UPDATE messages SET sendStatus = :targetStatus, failReason = :failReason WHERE sendStatus = :currentStatus and sentBytes == 0")
    suspend fun failUnstartedMessages(
        targetStatus: SendStatus = SendStatus.Failed,
        failReason: SendError = SendError.ConnectionFailed,
        currentStatus: SendStatus = SendStatus.Sending
    )

    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId AND isFromMe = 0 AND isRead = 0")
    suspend fun getUnreadCountBySessionId(sessionId: String): Int

    @Query(
        """
        SELECT localPath FROM messages 
        WHERE sessionId = :sessionId 
        AND localPath IS NOT NULL 
    """
    )
    suspend fun getLocalPathsBySessionId(sessionId: String): List<String>

    @Query(
        """
        SELECT id FROM messages 
        WHERE sessionId = :sessionId 
        AND (localPath IS NOT NULL OR fileSize > 0)
    """
    )
    suspend fun getTransferRelatedIdsBySessionId(sessionId: String): List<String>

    @Query("SELECT id FROM messages WHERE sessionId = :sessionId")
    suspend fun getIdsBySessionId(sessionId: String): List<String>

    @Query("SELECT id FROM messages")
    suspend fun getAllIds(): List<String>

    @Query(
        """
      SELECT localPath FROM messages 
      WHERE id IN (:ids) 
      AND localPath IS NOT NULL
    """
    )
    suspend fun getLocalPathsByIds(ids: Set<String>): List<String>

    @Query("SELECT localPath FROM messages WHERE localPath IS NOT NULL")
    suspend fun getAllLocalPaths(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE localPath = :localPath)")
    suspend fun hasLocalPathReference(localPath: String): Boolean

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId order by timestamp desc LIMIT 1")
    suspend fun getLatestMessage(sessionId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND contentType = :type")
    suspend fun getBySessionAndType(
        sessionId: String,
        type: MessageType
    ): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE contentType = :type AND localPath IS NOT NULL AND isRecalled = 0 ORDER BY timestamp DESC")
    suspend fun getAllByType(type: MessageType): List<MessageEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE id = :messageId)")
    suspend fun exists(messageId: String): Boolean

    @Transaction
    suspend fun update(messageId: String, updateBlock: (MessageEntity) -> MessageEntity) {
        val old = getById(messageId) ?: return
        val new = updateBlock(old).copy(
            audit = old.audit.copy(updatedAt = System.currentTimeMillis())
        )
        update(new)
    }

    @Query("UPDATE messages SET isRead = 1, updatedAt = :now WHERE sessionId = :sessionId AND isRead = 0 AND isFromMe = 0")
    suspend fun markAsReadBySessionId(sessionId: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: String)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)

    @Query("DELETE FROM messages WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: Set<String>)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Query("UPDATE messages SET localPath = NULL, sentBytes = 0 WHERE localPath IS NOT NULL")
    suspend fun clearAllLocalPaths()
}
