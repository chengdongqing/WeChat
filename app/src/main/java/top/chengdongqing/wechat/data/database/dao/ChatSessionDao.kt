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

    @Query("SELECT * FROM chat_sessions ORDER BY isPinned DESC, lastMessageTime DESC")
    fun observeAll(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE sessionId = :sessionId")
    fun observeById(sessionId: String): Flow<ChatSessionEntity?>

    @Query("SELECT * FROM chat_sessions WHERE sessionId = :sessionId")
    suspend fun getById(sessionId: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ChatSessionEntity)

    @Update
    suspend fun update(session: ChatSessionEntity)

    @Query("UPDATE chat_sessions SET unreadCount = unreadCount + 1 WHERE sessionId = :sessionId")
    suspend fun incrementUnreadCount(sessionId: String)

    @Query("UPDATE chat_sessions SET unreadCount = 0 WHERE sessionId = :sessionId")
    suspend fun clearUnreadCount(sessionId: String)

    @Query("UPDATE chat_sessions SET draftMessage = :draft WHERE sessionId = :sessionId")
    suspend fun updateDraft(sessionId: String, draft: String?)

    @Delete
    suspend fun delete(session: ChatSessionEntity)

    @Query("UPDATE chat_sessions SET isPinned = :isPinned WHERE sessionId = :sessionId")
    suspend fun updatePin(sessionId: String, isPinned: Boolean)

    @Query(
        """
        UPDATE chat_sessions 
        SET lastMessage = :lastMessage,
            lastMessageType = :lastMessageType,
            lastMessageTime = :timestamp,
            updatedAt = :timestamp
        WHERE sessionId = :sessionId
    """
    )
    suspend fun updateLastMessage(
        sessionId: String,
        lastMessage: String,
        lastMessageType: MessageType?,
        timestamp: Long
    )
}