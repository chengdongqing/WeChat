package top.chengdongqing.wechat.data.session

import androidx.room.withTransaction
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.MediaFileDao
import top.chengdongqing.wechat.data.database.entity.MediaFileEntity
import javax.inject.Inject

/**
 * 文件引用管理器
 */
class FileReferenceManager @Inject constructor(
    private val database: WeDatabase,
    private val mediaFileDao: MediaFileDao
) {

    /**
     * 注册文件引用（消息插入时调用）
     * 若文件已存在则引用计数 +1
     */
    suspend fun retain(localPath: String?, checksum: String) {
        localPath ?: return
        // 先尝试插入（已存在则忽略），再 +1
        // 两步合起来等价于 upsert
        database.withTransaction {
            mediaFileDao.insertIfAbsent(
                MediaFileEntity(
                    localPath = localPath,
                    refCount = 0,
                    checksum = checksum
                )
            )
            mediaFileDao.increment(localPath)
        }
    }

    /**
     * 释放单条消息的文件引用，并返回需要物理删除的路径
     */
    suspend fun release(localPath: String?): String? {
        localPath ?: return null
        return database.withTransaction {
            mediaFileDao.release(localPath)
            val unreferenced = mediaFileDao.getUnreferencedPaths()
            if (unreferenced.isNotEmpty()) {
                mediaFileDao.deleteUnreferenced()
            }
            unreferenced.firstOrNull { it == localPath }
        }
    }

    /**
     * 批量释放文件引用，返回所有需要物理删除的路径
     */
    suspend fun releaseAll(paths: Collection<String?>): List<String> {
        val countByPath = paths.filterNotNull()
            .groupingBy { it }
            .eachCount()
        if (countByPath.isEmpty()) return emptyList()

        return database.withTransaction {
            countByPath.forEach { (path, count) ->
                mediaFileDao.release(path, count)
            }
            val unreferenced = mediaFileDao.getUnreferencedPaths()
            if (unreferenced.isNotEmpty()) {
                mediaFileDao.deleteUnreferenced()
            }
            unreferenced
        }
    }
}