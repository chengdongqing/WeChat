package top.chengdongqing.wechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.database.entity.ConnectionInfoEntity
import top.chengdongqing.wechat.data.database.entity.mergeWith

@Dao
interface ConnectionInfoDao : BaseDao<ConnectionInfoEntity> {

    @Transaction
    suspend fun insertOrUpdate(entity: ConnectionInfoEntity) {
        val existing = getById(entity.userId)
        if (existing == null) {
            insert(entity)
        } else {
            update(entity.mergeWith(existing))
        }
    }

    @Query("SELECT * FROM connection_info WHERE userId = :userId")
    suspend fun getById(userId: String): ConnectionInfoEntity?

    @Query("SELECT isOnline FROM connection_info WHERE userId = :userId")
    fun observeOnlineStatus(userId: String): Flow<Boolean?>

    @Query("UPDATE connection_info SET isOnline = 0, updatedAt = :now WHERE userId = :userId")
    suspend fun markOffline(userId: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE connection_info SET isOnline = 1, lastSeen = :timestamp, updatedAt = :now WHERE userId = :userId")
    suspend fun markOnline(userId: String, timestamp: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM connection_info WHERE userId = :userId")
    suspend fun deleteById(userId: String)
}