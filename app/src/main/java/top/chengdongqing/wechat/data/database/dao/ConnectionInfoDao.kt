package top.chengdongqing.wechat.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.database.entity.ConnectionInfoEntity

@Dao
interface ConnectionInfoDao {

    @Query("SELECT * FROM connection_info WHERE userId = :userId")
    fun observeByUserId(userId: String): Flow<List<ConnectionInfoEntity>>

    @Query("SELECT * FROM connection_info WHERE userId = :userId ORDER BY priority ASC")
    suspend fun getConnectionsByUserId(userId: String): List<ConnectionInfoEntity>

    @Query("SELECT * FROM connection_info WHERE isOnline = 1")
    fun observeOnlineUsers(): Flow<List<ConnectionInfoEntity>>

    @Query("SELECT isOnline FROM connection_info WHERE userId = :userId")
    fun observeOnlineStatus(userId: String): Flow<Boolean?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(info: ConnectionInfoEntity)

    @Update
    suspend fun update(info: ConnectionInfoEntity)

    @Query("UPDATE connection_info SET isOnline = 0 WHERE userId = :userId")
    suspend fun markOffline(userId: String)

    @Query("UPDATE connection_info SET isOnline = 1, lastSeen = :timestamp WHERE userId = :userId")
    suspend fun markOnline(userId: String, timestamp: Long)

    @Delete
    suspend fun delete(info: ConnectionInfoEntity)
}