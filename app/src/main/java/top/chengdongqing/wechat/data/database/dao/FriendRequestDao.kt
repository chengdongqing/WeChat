package top.chengdongqing.wechat.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.database.entity.FriendRequestEntity
import top.chengdongqing.wechat.data.database.entity.RequestDirection
import top.chengdongqing.wechat.data.database.entity.RequestStatus

@Dao
interface FriendRequestDao {

    @Query("SELECT * FROM friend_requests ORDER BY createAt DESC")
    fun getAll(): Flow<List<FriendRequestEntity>>

    @Query("SELECT * FROM friend_requests WHERE id = :requestId")
    suspend fun getById(requestId: String): FriendRequestEntity?

    @Query(
        """
        SELECT * FROM friend_requests 
        WHERE peerUserId = :peerUserId 
        AND direction = :direction 
        ORDER BY createAt DESC 
        LIMIT 1
    """
    )
    suspend fun getByPeerUserId(
        peerUserId: String,
        direction: RequestDirection
    ): FriendRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: FriendRequestEntity)

    @Update
    suspend fun update(request: FriendRequestEntity)

    @Query("UPDATE friend_requests SET status = :status, updatedAt = :updatedAt WHERE id = :requestId")
    suspend fun updateStatus(requestId: String, status: RequestStatus, updatedAt: Long)

    @Query("DELETE FROM friend_requests WHERE id = :requestId")
    suspend fun delete(requestId: String)

    @Query("SELECT COUNT(*) FROM friend_requests WHERE direction = :direction AND status = :status")
    fun getPendingCount(
        direction: RequestDirection = RequestDirection.Incoming,
        status: RequestStatus = RequestStatus.Pending
    ): Flow<Int>

    // 查询未读数量（只统计收到的待处理申请）
    @Query(
        """
        SELECT COUNT(*) FROM friend_requests 
        WHERE direction = :direction 
        AND status = :status 
        AND isRead = 0
    """
    )
    fun observeUnreadCount(
        direction: RequestDirection = RequestDirection.Incoming,
        status: RequestStatus = RequestStatus.Pending
    ): Flow<Int>

    // 标记所有收到的申请为已读
    @Query(
        """
        UPDATE friend_requests 
        SET isRead = 1, updatedAt = :updatedAt 
        WHERE direction = :direction
    """
    )
    suspend fun markAllIncomingAsRead(
        updatedAt: Long,
        direction: RequestDirection = RequestDirection.Incoming
    )
}