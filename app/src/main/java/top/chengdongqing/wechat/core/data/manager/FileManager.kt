package top.chengdongqing.wechat.core.data.manager

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件管理器
 * 负责头像等文件的存储和管理
 */
@Singleton
class FileManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val avatarDir: File
        get() = File(context.filesDir, "avatars").apply {
            if (!exists()) mkdirs()
        }

    /**
     * 保存头像文件
     * @param sourceUri 源图片URI
     * @param userId 用户ID
     * @return 保存后的文件路径
     */
    suspend fun saveAvatar(sourceUri: Uri, userId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // 生成文件名：userId_timestamp.jpg
                val fileName = "${userId}_${System.currentTimeMillis()}.jpg"
                val targetFile = File(avatarDir, fileName)

                // 复制文件
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext Result.failure(Exception("无法打开图片文件"))

                Result.success(targetFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 删除头像文件
     */
    suspend fun deleteAvatar(avatarPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(avatarPath)
            if (file.exists()) {
                file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取头像URI
     */
    fun getAvatarUri(avatarPath: String?): Uri? {
        if (avatarPath == null) return null
        val file = File(avatarPath)
        return if (file.exists()) Uri.fromFile(file) else null
    }

    /**
     * 清理所有头像文件
     */
    suspend fun clearAllAvatars(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            avatarDir.listFiles()?.forEach { it.delete() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}