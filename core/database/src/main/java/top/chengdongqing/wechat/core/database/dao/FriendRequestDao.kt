package top.chengdongqing.wechat.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.database.entity.FriendRequestEntity
import top.chengdongqing.wechat.core.model.FriendRequestStatus

@Dao
interface FriendRequestDao : BaseDao<FriendRequestEntity> {

    @Query("SELECT * FROM friend_requests ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FriendRequestEntity>>

    @Query("SELECT * FROM friend_requests WHERE id = :id")
    suspend fun getById(id: String): FriendRequestEntity?

    @Query("SELECT COUNT(*) FROM friend_requests WHERE isFromMe = :isFromMe AND status = :status")
    fun getPendingCount(
        isFromMe: Boolean = false,
        status: FriendRequestStatus = FriendRequestStatus.Pending
    ): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM friend_requests 
        WHERE isFromMe = :isFromMe 
        AND status = :status 
        AND isRead = 0
    """
    )
    fun observeUnreadCount(
        isFromMe: Boolean = false,
        status: FriendRequestStatus = FriendRequestStatus.Pending
    ): Flow<Int>

    @Query(
        """
        UPDATE friend_requests 
        SET isRead = 1, updatedAt = :now 
        WHERE isFromMe = :isFromMe
    """
    )
    suspend fun markAllIncomingAsRead(
        isFromMe: Boolean = false,
        now: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE friend_requests 
        SET status = :expiredStatus 
        WHERE status = :pendingStatus 
        AND createdAt < :beforeTime
    """
    )
    suspend fun markExpired(
        beforeTime: Long,
        expiredStatus: FriendRequestStatus,
        pendingStatus: FriendRequestStatus
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