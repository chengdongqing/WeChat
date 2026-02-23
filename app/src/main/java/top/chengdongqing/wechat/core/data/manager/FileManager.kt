package top.chengdongqing.wechat.core.data.manager

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.data.database.entity.MessageType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件管理器
 * 负责头像、聊天媒体文件等的存储和管理
 */
@Singleton
class FileManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val avatarDir: File
        get() = File(context.filesDir, "avatars").apply {
            if (!exists()) mkdirs()
        }

    private val imagesDir: File
        get() = File(context.filesDir, "images").apply {
            if (!exists()) mkdirs()
        }

    private val videosDir: File
        get() = File(context.filesDir, "videos").apply {
            if (!exists()) mkdirs()
        }

    private val audiosDir: File
        get() = File(context.filesDir, "audios").apply {
            if (!exists()) mkdirs()
        }

    private val filesDir: File
        get() = File(context.filesDir, "files").apply {
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

    /**
     * 保存图片
     */
    suspend fun saveImage(sourceUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // 生成文件名：timestamp.jpg
                val fileName = "${System.currentTimeMillis()}.jpg"
                val targetFile = File(imagesDir, fileName)

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
     * 保存媒体文件（图片/视频/音频/文件）
     *
     * @param messageType 消息类型
     * @param sourceFile 源文件
     * @param messageId 消息ID（用于文件命名）
     * @param extension 文件扩展名（可选，如 "jpg", "mp4"）
     * @return 保存后的文件绝对路径
     */
    suspend fun saveMediaFile(
        messageType: MessageType,
        sourceFile: File,
        messageId: String,
        extension: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 根据消息类型选择目录
            val targetDir = when (messageType) {
                MessageType.Image -> imagesDir
                MessageType.Video -> videosDir
                MessageType.Voice -> audiosDir
                MessageType.File -> filesDir
                else -> filesDir // 默认使用 files 目录
            }

            // 生成文件名：messageId + 扩展名
            val fileName = if (extension != null) {
                "${messageId}.${extension.trimStart('.')}"
            } else {
                messageId
            }

            val targetFile = File(targetDir, fileName)

            // 写入文件数据
            FileInputStream(sourceFile).channel.use { sourceChannel ->
                FileOutputStream(targetFile).channel.use { targetChannel ->
                    sourceChannel.transferTo(0, sourceChannel.size(), targetChannel)
                }
            }

            Result.success(targetFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除媒体文件
     *
     * @param filePath 文件绝对路径
     */
    suspend fun deleteMediaFile(filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 清理所有媒体文件
     */
    suspend fun clearAllMediaFiles(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            imagesDir.listFiles()?.forEach { it.delete() }
            videosDir.listFiles()?.forEach { it.delete() }
            audiosDir.listFiles()?.forEach { it.delete() }
            filesDir.listFiles()?.forEach { it.delete() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}