package top.chengdongqing.wechat.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.database.entity.ChatSessionEntity
import top.chengdongqing.wechat.data.database.entity.MessageType

@Dao
interface ChatSessionDao {

    @Query("SELECT * FROM chat_sessions WHERE isHidden = 0 ORDER BY isPinned DESC, lastMessageTime DESC")
    fun observeAll(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE sessionId = :sessionId")
    fun observeById(sessionId: String): Flow<ChatSessionEntity?>

    @Query("SELECT * FROM chat_sessions WHERE sessionId = :sessionId")
    suspend fun getById(sessionId: String): ChatSessionEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM chat_sessions WHERE sessionId = :sessionId)")
    suspend fun exists(sessionId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ChatSessionEntity)

    @Update
    suspend fun update(session: ChatSessionEntity)

    @Query("UPDATE chat_sessions SET unreadCount = unreadCount + 1 WHERE sessionId = :sessionId")
    suspend fun incrementUnreadCount(sessionId: String)

    @Query("UPDATE chat_sessions SET unreadCount = 0 WHERE sessionId = :sessionId")
    suspend fun clearUnreadCount(sessionId: String)

    @Query("UPDATE chat_sessions SET unreadCount = 1 WHERE sessionId = :sessionId")
    suspend fun markAsUnread(sessionId: String)

    @Query("UPDATE chat_sessions SET draftMessage = :draft WHERE sessionId = :sessionId")
    suspend fun updateDraft(sessionId: String, draft: String?)

    @Query("UPDATE chat_sessions SET isHidden = 1 WHERE sessionId = :sessionId")
    suspend fun hideSession(sessionId: String)

    @Delete
    suspend fun delete(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE sessionId = :sessionId")
    suspend fun deleteById(sessionId: String)

    @Query("UPDATE chat_sessions SET isPinned = :isPinned WHERE sessionId = :sessionId")
    suspend fun updatePin(sessionId: String, isPinned: Boolean)

    @Query("UPDATE chat_sessions SET isMuted = :isMuted, updatedAt = :now WHERE sessionId = :sessionId")
    suspend fun updateMute(
        sessionId: String,
        isMuted: Boolean,
        now: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE chat_sessions 
        SET lastMessage = :lastMessage,
            lastMessageType = :lastMessageType,
            lastMessageTime = :timestamp,
            updatedAt = :timestamp,
            -- 如果隐藏了，则取消隐藏
            isHidden = CASE WHEN isHidden = 1 THEN 0 ELSE isHidden END
        WHERE sessionId = :sessionId
        """
    )
    suspend fun updateLastMessage(
        sessionId: String,
        lastMessage: String,
        lastMessageType: MessageType?,
        timestamp: Long
    )

    @Query(
        """
        UPDATE chat_sessions 
        SET lastMessage = NULL, 
            unreadCount = 0,
            updatedAt = :now 
        WHERE sessionId = :sessionId
    """
    )
    suspend fun clearLastMessage(sessionId: String, now: Long = System.currentTimeMillis())
}