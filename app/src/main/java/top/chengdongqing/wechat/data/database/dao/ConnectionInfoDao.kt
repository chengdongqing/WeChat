package top.chengdongqing.wechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.database.entity.ConnectionInfoEntity
import top.chengdongqing.wechat.data.network.connection.ConnectionMode

@Dao
interface ConnectionInfoDao : BaseDao<ConnectionInfoEntity> {

    @Query("SELECT * FROM connection_info WHERE userId = :userId ORDER BY priority ASC")
    suspend fun getById(userId: String): ConnectionInfoEntity?

    @Query("select * from connection_info where connectionMode = :connectionMode")
    suspend fun getByMode(connectionMode: ConnectionMode): List<ConnectionInfoEntity>

    @Query("SELECT isOnline FROM connection_info WHERE userId = :userId")
    fun observeOnlineStatus(userId: String): Flow<Boolean?>

    @Transaction
    suspend fun update(
        userId: String,
        updateBlock: (ConnectionInfoEntity) -> ConnectionInfoEntity
    ) {
        val old = getById(userId) ?: return
        val new = updateBlock(old).copy(
            audit = old.audit.copy(updatedAt = System.currentTimeMillis())
        )
        update(new)
    }

    @Query("UPDATE connection_info SET isOnline = 0, updatedAt = :now WHERE userId = :userId")
    suspend fun markOffline(userId: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE connection_info SET isOnline = 1, lastSeen = :timestamp, updatedAt = :now WHERE userId = :userId")
    suspend fun markOnline(userId: String, timestamp: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM connection_info WHERE userId = :userId")
    suspend fun deleteById(userId: String)
}