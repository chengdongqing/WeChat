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

    /**
     * 引用计数 -1，返回更新后的计数
     * 若计数已为 0 则不更新（防止负数）
     */
    @Query(
        """
        UPDATE media_files
        SET refCount = refCount - 1
        WHERE localPath = :localPath AND refCount > 0
    """
    )
    suspend fun release(localPath: String)

    @Query(
        """
        UPDATE media_files
        SET refCount = refCount - 1
        WHERE localPath IN (:paths) AND refCount > 0
    """
    )
    suspend fun releaseAll(paths: Collection<String>)

    @Query("SELECT localPath FROM media_files WHERE refCount <= 0")
    suspend fun getUnreferencedPaths(): List<String>

    @Query("DELETE FROM media_files WHERE refCount <= 0")
    suspend fun deleteUnreferenced()
}