package top.chengdongqing.wechat.core.file.cache

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 清理所有缓存
 */
suspend fun Context.clearAllCaches() = withContext(Dispatchers.IO) {
    val protectedDirs = listOf("transfers") // 不清理的目录

    try {
        // 清理内部缓存 (/data/user/0/包名/cache)
        deleteDirContent(cacheDir, protectedDirs)
        // 清理外部缓存 (/sdcard/Android/data/包名/cache)
        deleteDirContent(externalCacheDir, protectedDirs)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 删除目录下的内容
 */
private fun deleteDirContent(dir: File?, excludeNames: List<String>): Boolean {
    if (dir == null || !dir.exists() || !dir.isDirectory) return false

    dir.listFiles()?.forEach { child ->
        // 检查当前文件/文件夹名称是否在排除列表中
        if (child.name !in excludeNames) {
            child.deleteRecursively()
        }
    }
    return true
}
