package top.chengdongqing.wechat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    // 微信聊天列表最重要的就是这个：按时间升序排列，并返回 Flow 实现实时刷新
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    // 更新消息状态（比如从“发送中”变为“已送达”）
    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: Int)

    @Query("UPDATE messages SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float)

    // 微信的删除聊天记录
    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}