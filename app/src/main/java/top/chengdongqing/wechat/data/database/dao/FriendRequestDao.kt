package top.chengdongqing.wechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.database.entity.FriendRequestEntity
import top.chengdongqing.wechat.data.database.entity.RequestDirection
import top.chengdongqing.wechat.data.database.entity.RequestStatus

@Dao
interface FriendRequestDao : BaseDao<FriendRequestEntity> {

    @Query("SELECT * FROM friend_requests ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FriendRequestEntity>>

    @Query("SELECT * FROM friend_requests WHERE id = :requestId")
    suspend fun getById(requestId: String): FriendRequestEntity?

    @Query(
        """
        SELECT * FROM friend_requests 
        WHERE peerId = :peerId 
        AND direction = :direction 
        ORDER BY createdAt DESC 
        LIMIT 1
    """
    )
    suspend fun getByPeerId(
        peerId: String,
        direction: RequestDirection
    ): FriendRequestEntity?

    @Query("SELECT COUNT(*) FROM friend_requests WHERE direction = :direction AND status = :status")
    fun getPendingCount(
        direction: RequestDirection = RequestDirection.Incoming,
        status: RequestStatus = RequestStatus.Pending
    ): Flow<Int>

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

    @Query(
        """
        UPDATE friend_requests 
        SET isRead = 1, updatedAt = :now 
        WHERE direction = :direction
    """
    )
    suspend fun markAllIncomingAsRead(
        direction: RequestDirection = RequestDirection.Incoming,
        now: Long = System.currentTimeMillis()
    )

    @Transaction
    suspend fun update(
        requestId: String,
        updateBlock: (FriendRequestEntity) -> FriendRequestEntity
    ) {
        val old = getById(requestId) ?: return
        val new = updateBlock(old).copy(
            audit = old.audit.copy(updatedAt = System.currentTimeMillis())
        )
        update(new)
    }

    @Query("DELETE FROM friend_requests WHERE id = :requestId")
    suspend fun deleteById(requestId: String)
}