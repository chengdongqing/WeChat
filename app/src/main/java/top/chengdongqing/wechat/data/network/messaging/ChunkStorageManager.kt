package top.chengdongqing.wechat.data.network.messaging

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.network.model.ChatProtocol
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.open
import java.nio.file.StandardOpenOption
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 分片存储管理器
 *
 * 负责媒体文件的分片写入、断点恢复、合并和清理。
 *
 * 目录结构：
 * ```
 * cache/transfers/
 *   └── {messageId}/
 *       ├── meta.json       // 序列化的 MediaMessage 元数据
 *       ├── chunk_0000
 *       ├── chunk_0001
 *       └── ...
 * ```
 */
@Singleton
class ChunkStorageManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json
) {
    private companion object {
        const val TAG = "ChunkStorageManager"
        const val TRANSFERS_DIR = "transfers"
        const val META_FILE = "meta.json"
        const val DATA_FILE = "data.tmp"
    }

    private val transfersRoot: File
        get() = File(context.cacheDir, TRANSFERS_DIR)

    private fun getTransferDir(messageId: String): File =
        File(transfersRoot, messageId)

    /**
     * 初始化传输：创建目录、保存元数据、预分配文件、打开写入会话
     *
     * @return WriteSession，调用方持有并在传输期间复用
     */
    suspend fun initTransfer(
        metadata: ChatProtocol.MediaMessage
    ): WriteSession = withContext(Dispatchers.IO) {
        val dir = getTransferDir(metadata.messageId)
        dir.mkdirs()

        // 保存元数据（调试用）
        File(dir, META_FILE).writeText(json.encodeToString(metadata))

        // 预分配文件 + 打开 FileChannel
        val dataFile = File(dir, DATA_FILE)
        val channel = open(
            dataFile.toPath(),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.READ
        )
        // 预分配空间
        channel.position(metadata.fileSize - 1)
        channel.write(ByteBuffer.wrap(byteArrayOf(0)))
        channel.position(0)

        Log.d(TAG, "预分配文件: messageId=${metadata.messageId}, size=${metadata.fileSize}")
        WriteSession(channel)
    }

    /**
     * 为断点续传打开已有文件的写入会话
     *
     * @param messageId 消息 ID
     * @param offset 续传起始偏移，FileChannel 会定位到此处
     * @return WriteSession，如果文件不存在返回 null
     */
    suspend fun resumeTransfer(
        messageId: String,
        offset: Long
    ): WriteSession? = withContext(Dispatchers.IO) {
        val dataFile = File(getTransferDir(messageId), DATA_FILE)
        if (!dataFile.exists()) return@withContext null

        val channel = open(
            dataFile.toPath(),
            StandardOpenOption.WRITE,
            StandardOpenOption.READ
        )
        channel.position(offset)

        Log.d(TAG, "恢复写入会话: messageId=$messageId, offset=$offset")
        WriteSession(channel)
    }

    /**
     * 是否有未完成的传输
     */
    suspend fun hasPendingTransfer(messageId: String): Boolean = withContext(Dispatchers.IO) {
        File(getTransferDir(messageId), DATA_FILE).exists()
    }

    /**
     * 获取完成后的数据文件
     */
    fun getCompletedFile(messageId: String): File =
        File(getTransferDir(messageId), DATA_FILE)

    /**
     * 清理指定消息的传输目录
     */
    suspend fun cleanup(messageId: String) = withContext(Dispatchers.IO) {
        val dir = getTransferDir(messageId)
        if (dir.exists()) {
            dir.deleteRecursively()
            Log.d(TAG, "传输目录已清理: $messageId")
        }
    }

    /**
     * 清理超时的未完成传输
     */
    suspend fun cleanupStaleTransfers(maxAgeMs: Long = 24 * 60 * 60 * 1000L) =
        withContext(Dispatchers.IO) {
            val root = transfersRoot
            if (!root.exists()) return@withContext

            val cutoff = System.currentTimeMillis() - maxAgeMs
            root.listFiles()?.forEach { dir ->
                val metaFile = File(dir, META_FILE)
                if (metaFile.exists() && metaFile.lastModified() < cutoff) {
                    dir.deleteRecursively()
                    Log.d(TAG, "清理过期传输: ${dir.name}")
                }
            }
        }
}

/**
 * 写入会话
 *
 * 持有 FileChannel，在整个传输过程中复用同一个句柄。
 * 传输完成或中断时调用 [close] 释放资源。
 */
class WriteSession(private val channel: FileChannel) {

    /**
     * 在指定偏移量写入数据
     */
    fun writeAtOffset(offset: Long, data: ByteArray) {
        channel.position(offset)
        channel.write(ByteBuffer.wrap(data))
    }

    /**
     * 强制刷盘
     */
    fun flush() {
        runCatching { channel.force(false) }
    }

    /**
     * 关闭文件句柄
     */
    fun close() {
        runCatching { channel.close() }
    }
}