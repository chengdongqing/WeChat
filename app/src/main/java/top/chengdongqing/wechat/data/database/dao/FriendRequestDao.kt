package top.chengdongqing.wechat.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.database.entity.FriendRequestEntity
import top.chengdongqing.wechat.data.database.entity.RequestStatus

@Dao
interface FriendRequestDao {

    @Query("SELECT * FROM friend_requests ORDER BY createAt DESC")
    fun getAll(): Flow<List<FriendRequestEntity>>

    @Query("SELECT * FROM friend_requests WHERE id = :requestId")
    suspend fun getById(requestId: String): FriendRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: FriendRequestEntity)

    @Update
    suspend fun update(request: FriendRequestEntity)

    @Query("UPDATE friend_requests SET status = :status, updatedAt = :updatedAt WHERE id = :requestId")
    suspend fun updateStatus(requestId: String, status: RequestStatus, updatedAt: Long)

    @Query("DELETE FROM friend_requests WHERE id = :requestId")
    suspend fun delete(requestId: String)

    @Query("SELECT COUNT(*) FROM friend_requests WHERE direction = 'INCOMING' AND status = 'PENDING'")
    fun getPendingCount(): Flow<Int>
}