package top.chengdongqing.wechat.core.file

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.util.FileNameUtils
import top.chengdongqing.wechat.data.database.entity.MessageType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用私有文件管理器
 *
 * 职责：
 * - 管理应用私有目录的文件存储
 * - 提供统一的文件保存、删除、清理接口
 * - 自动处理文件格式检测和命名
 */
@Singleton
class FileManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /**
     * 头像目录
     */
    private val avatarDir: File
        get() = ensureDir("avatars")

    /**
     * 图片目录
     */
    private val imagesDir: File
        get() = ensureDir("images")

    /**
     * 视频目录
     */
    private val videosDir: File
        get() = ensureDir("videos")

    /**
     * 音频目录
     */
    private val audiosDir: File
        get() = ensureDir("audios")

    /**
     * 文件目录
     */
    private val filesDir: File
        get() = ensureDir("files")

    /**
     * 确保目录存在
     */
    private fun ensureDir(name: String): File {
        return File(context.filesDir, name).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * 根据目录名获取 File
     */
    private fun getDirectory(dirName: String): File {
        return when (dirName) {
            "avatars" -> avatarDir
            "images" -> imagesDir
            "videos" -> videosDir
            "audios" -> audiosDir
            "files" -> filesDir
            else -> filesDir
        }
    }

    /**
     * 保存头像文件
     *
     * @param sourceUri 源图片URI
     * @param userId 用户ID
     * @return 保存后的文件绝对路径
     */
    suspend fun saveAvatar(sourceUri: Uri, userId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val fileName = "${userId}_${System.currentTimeMillis()}.jpg"
                val targetFile = File(avatarDir, fileName)

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
    suspend fun deleteAvatar(avatarPath: String): Result<Unit> =
        deleteFile(avatarPath)

    /**
     * 保存媒体文件
     *
     * 自动检测图片格式，使用正确的扩展名
     *
     * @param messageType 消息类型
     * @param sourceFile 源文件
     * @param extension 文件扩展名（可选，会自动检测）
     * @return 保存后的文件绝对路径
     */
    suspend fun saveMediaFile(
        messageType: MessageType,
        sourceFile: File,
        extension: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) {
                return@withContext Result.failure(IllegalStateException("源文件不存在"))
            }

            val config = FileNameUtils.getFileConfig(messageType)
            val targetDir = getDirectory(config.dirName)

            /**
             * 确定最终扩展名
             * 优先级：手动指定 > 检测格式 > 源文件扩展名 > 默认扩展名
             */
            val finalExtension = when {
                !extension.isNullOrBlank() -> extension.trimStart('.')
                messageType == MessageType.Image -> FileNameUtils.detectImageFormat(sourceFile)
                sourceFile.extension.isNotBlank() -> sourceFile.extension
                else -> config.extension
            }

            val fileName = FileNameUtils.generateFileName(config.prefix, finalExtension)
            val targetFile = File(targetDir, fileName)

            /**
             * 使用 NIO Channel 高效复制
             */
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
     * 从 Uri 保存媒体文件
     *
     * @param messageType 消息类型
     * @param sourceUri 源 Uri
     * @param extension 文件扩展名（可选）
     * @return 保存后的文件绝对路径
     */
    suspend fun saveMediaFileFromUri(
        messageType: MessageType,
        sourceUri: Uri,
        extension: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val config = FileNameUtils.getFileConfig(messageType)
            val targetDir = getDirectory(config.dirName)

            val finalExtension = extension?.trimStart('.') ?: config.extension
            val fileName = FileNameUtils.generateFileName(config.prefix, finalExtension)
            val targetFile = File(targetDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("无法打开源文件"))

            /**
             * 图片格式检测和重命名
             */
            if (messageType == MessageType.Image && extension == null) {
                val detectedExt = FileNameUtils.detectImageFormat(targetFile)
                if (detectedExt != finalExtension) {
                    val newFileName = FileNameUtils.generateFileName(
                        config.prefix,
                        detectedExt,
                        targetFile.nameWithoutExtension.substringAfter('_').toLongOrNull()
                            ?: System.currentTimeMillis()
                    )
                    val newFile = File(targetDir, newFileName)
                    targetFile.renameTo(newFile)
                    return@withContext Result.success(newFile.absolutePath)
                }
            }

            Result.success(targetFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除媒体文件
     */
    suspend fun deleteMediaFile(filePath: String): Result<Unit> =
        deleteFile(filePath)

    /**
     * 删除文件（通用）
     */
    private suspend fun deleteFile(filePath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
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
     * 批量删除文件
     */
    suspend fun deleteFiles(filePaths: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                filePaths.forEach { path ->
                    File(path).takeIf { it.exists() }?.delete()
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
            listOf(avatarDir, imagesDir, videosDir, audiosDir, filesDir).forEach { dir ->
                dir.listFiles()?.forEach { it.delete() }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取目录大小（字节）
     */
    suspend fun getDirectorySize(messageType: MessageType): Long =
        withContext(Dispatchers.IO) {
            val config = FileNameUtils.getFileConfig(messageType)
            val dir = getDirectory(config.dirName)
            dir.walk().filter { it.isFile }.sumOf { it.length() }
        }
}