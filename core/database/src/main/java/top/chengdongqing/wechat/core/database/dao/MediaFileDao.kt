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

    @Query("SELECT * FROM media_files WHERE localPath = :localPath LIMIT 1")
    suspend fun getByPath(localPath: String): MediaFileEntity?

    @Query("SELECT localPath FROM media_files")
    suspend fun getAllPaths(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: MediaFileEntity)

    @Query("DELETE FROM media_files WHERE localPath = :localPath")
    suspend fun deleteByPath(localPath: String)

    @Query("UPDATE media_files SET checksum = :checksum WHERE localPath = :localPath AND checksum = ''")
    suspend fun fillChecksum(localPath: String, checksum: String)

}
