package top.chengdongqing.wechat.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import top.chengdongqing.wechat.core.database.entity.MediaFileEntity

@Dao
interface MediaFileDao {

    @Query("select * from media_files where checksum = :checksum limit 1")
    suspend fun getByChecksum(checksum: String): MediaFileEntity?

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