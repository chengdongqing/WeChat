package top.chengdongqing.wechat.core.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.util.extractExtension
import top.chengdongqing.wechat.core.util.generateFileName
import top.chengdongqing.wechat.core.util.getFileConfig
import top.chengdongqing.wechat.core.util.getFileMetadata
import top.chengdongqing.wechat.data.model.MessageType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
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
class PrivateFileManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "PrivateFileManager"
    }

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
    private val recordingsDir: File
        get() = ensureDir("recordings")

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
    private fun getDirectory(dirName: String): File = when (dirName) {
        "avatars" -> avatarDir
        "images" -> imagesDir
        "videos" -> videosDir
        "recordings" -> recordingsDir
        else -> filesDir
    }

    /**
     * 保存头像文件
     *
     * @param userId 用户ID
     * @param sourceUri 源图片URI
     * @return 保存后的文件绝对路径
     */
    suspend fun saveAvatar(userId: String, sourceUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val fileName = "${userId}_${System.currentTimeMillis()}.jpg"
                val targetFile = File(avatarDir, fileName)

                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IOException("文件打开失败")

                targetFile.absolutePath
            }.onFailure {
                Log.e(TAG, "保存头像失败", it)
            }
        }

    /**
     * 将头像字节数据保存为文件
     *
     * @param userId 用户ID，用于生成唯一文件名
     * @param sourceBytes 图片字节数据
     * @param isThumbnail 是否为缩略图，影响文件命名
     * @return 保存后的文件绝对路径
     */
    suspend fun saveAvatar(
        userId: String,
        sourceBytes: ByteArray,
        isThumbnail: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size)
                ?: throw IOException("图片解析失败")

            val suffix = if (isThumbnail) "_thumb" else ""
            val fileName = "${userId}${suffix}_${System.currentTimeMillis()}.jpg"
            val targetFile = File(avatarDir, fileName)

            targetFile.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }.also {
                bitmap.recycle()
            }

            targetFile.absolutePath
        }.onFailure {
            Log.e(TAG, "保存头像失败", it)
        }
    }

    /**
     * 保存媒体文件
     *
     * 自动检测图片格式，使用正确的扩展名
     *
     * @param messageType 消息类型
     * @param sourceFile 源文件
     * @param extension 文件扩展名
     * @return 保存后的文件绝对路径
     */
    suspend fun saveMedia(
        messageType: MessageType,
        sourceFile: File,
        extension: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!sourceFile.exists()) throw IllegalArgumentException("源文件不存在")

            val config = messageType.getFileConfig()
            val targetDir = getDirectory(config.dirName)
            val finalExtension = extension?.trimStart('.') // 优先使用传入的后缀名
                ?: sourceFile.extension // 通过文件名获取
            val fileName = generateFileName(config.prefix, finalExtension)
            val targetFile = File(targetDir, fileName)

            // 使用 NIO Channel 高效拷贝
            FileInputStream(sourceFile).channel.use { sourceChannel ->
                FileOutputStream(targetFile).channel.use { targetChannel ->
                    sourceChannel.transferTo(0, sourceChannel.size(), targetChannel)
                }
            }

            targetFile.absolutePath
        }.onFailure {
            Log.e(TAG, "保存文件失败", it)
        }
    }

    /**
     * 从 Uri 保存媒体文件
     *
     * @param messageType 消息类型
     * @param sourceUri 源 Uri
     * @param extension 文件扩展名
     * @return 保存后的文件绝对路径
     */
    suspend fun saveMedia(
        messageType: MessageType,
        sourceUri: Uri,
        extension: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val config = messageType.getFileConfig()
            val targetDir = getDirectory(config.dirName)

            val finalExtension = extension?.trimStart('.') // 优先使用传入的后缀名
                ?: context.getFileMetadata(sourceUri)?.filename.extractExtension() // 通过contentResolver查询后缀名
                ?: config.extension // 最后使用默认的
            val fileName = generateFileName(config.prefix, finalExtension)
            val targetFile = File(targetDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("无法打开源文件")

            targetFile.absolutePath
        }.onFailure {
            Log.e(TAG, "保存文件失败", it)
        }
    }

    /**
     * 删除文件
     */
    suspend fun deleteFile(filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            File(filePath).takeIf { it.exists() }?.delete()
            Unit
        }
    }

    /**
     * 批量删除文件
     */
    suspend fun deleteFiles(filePaths: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            filePaths.map { path ->
                async {
                    File(path).takeIf { it.exists() }?.delete()
                }
            }.awaitAll()
            Unit
        }
    }

    /**
     * 清理所有文件
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        context.filesDir.deleteRecursively()
        context.getExternalFilesDir(null)?.deleteRecursively()
    }

    /**
     * 获取目录大小（字节）
     */
    suspend fun getDirectorySize(messageType: MessageType): Long = withContext(Dispatchers.IO) {
        val config = messageType.getFileConfig()
        val dir = getDirectory(config.dirName)
        dir.walk().filter { it.isFile }.sumOf { it.length() }
    }
}