package top.chengdongqing.wechat.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import top.chengdongqing.wechat.data.database.entity.MediaFileEntity

@Dao
interface MediaFileDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: MediaFileEntity)

    @Query("UPDATE media_files SET refCount = refCount + 1 WHERE localPath = :localPath")
    suspend fun increment(localPath: String)

    @Query(
        """
        UPDATE media_files
        SET refCount = MAX(0, refCount - 1)
        WHERE localPath = :localPath AND refCount > 0
    """
    )
    suspend fun release(localPath: String)

    @Query(
        """
        UPDATE media_files
        SET refCount = MAX(0, refCount - :count)
        WHERE localPath = :path
    """
    )
    suspend fun release(path: String, count: Int)

    @Query("SELECT localPath FROM media_files WHERE refCount <= 0")
    suspend fun getUnreferencedPaths(): List<String>

    @Query("DELETE FROM media_files WHERE refCount <= 0")
    suspend fun deleteUnreferenced()
}