package top.chengdongqing.wechat.data.network.messaging

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.util.toMD5Hex
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.SendStatus
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.exception.ConnectionException
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketType
import top.chengdongqing.wechat.data.network.socket.SocketManager
import top.chengdongqing.wechat.data.network.transfer.WifiLockManager
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息发送器
 *
 * 性能优化:
 * - chunk 256KB，减少 Packet 头开销和 syscall 次数
 * - writeNoFlush: chunk 不逐片 flush，由 BufferedOutputStream 积满后自动推送，
 *   最后由 sendAtomicTransfer 统一 flush
 * - WifiLock: 后台传输期间保持 WiFi 高性能模式
 * - 进度回调节流: 每 1MB 报告一次，避免日志/UI 刷新过频
 *
 * 媒体发送流程:
 * 1. 流式读文件，边读边算 MD5，边发 FILE_CHUNK（单次遍历，零额外 I/O）
 * 2. 读完后将 MD5 写入 FILE_META 的 checksum 字段
 *
 * 注意: FILE_META 在所有 FILE_CHUNK 之前发送，但此时还不知道 MD5。
 * 解决方案: 先算 MD5，再发 META，再发 CHUNK。
 * 代价是文件被读两遍？不——我们把 MD5 计算和 chunk 发送合并:
 *
 * 实际方案: 先单独算一遍 MD5（流式，只读不存），再发 META（含 checksum），再发 CHUNK。
 * 对于 LAN 场景，磁盘顺序读 ~200MB/s，MD5 计算 ~400MB/s，
 * 100MB 文件额外读一遍只需 ~0.5s，网络传输本身要 ~1s，总开销增加 ~50%。
 *
 * 更优方案（选用）: 两遍合一——读文件时同时算 MD5 和发 chunk，
 * 但 META 里的 checksum 先留空发出去，chunk 全部发完后再补发一个校验包。
 * 这需要新增 Packet 类型，侵入性大。
 *
 * 最终选择: 先算 MD5 再发。简单可靠，额外耗时对 LAN 可接受。
 */
@Singleton
class MessageSender @Inject constructor(
    private val socketManager: SocketManager,
    private val connectionInfoDao: ConnectionInfoDao,
    private val messageDao: MessageDao,
    private val wifiLockManager: WifiLockManager,
    private val json: Json
) {
    private companion object {
        const val TAG = "MessageSender"
    }

    suspend fun sendTextMessage(message: MessageEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            sendSinglePacket(message) {
                val protocol = ChatProtocol.TextMessage(
                    messageId = message.messageId,
                    senderId = message.senderId,
                    receiverId = message.receiverId,
                    content = message.content,
                    timestamp = message.timestamp
                )
                Packet(PacketType.TEXT, serializePolymorphic(protocol))
            }
        }

    /**
     * 发送媒体消息
     *
     * 流程: 算 MD5 → 确保连接 → WiFi Lock → 原子发送 (META + CHUNK) → 释放
     */
    suspend fun sendMediaMessage(
        message: MessageEntity,
        file: File
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val fileSize = file.length()

            // 流式计算MD5
            val checksum = file.toMD5Hex()
            Log.d(TAG, "MD5 计算完成 [${message.messageId}]: $checksum")

            // 确保连接
            ensureConnected(message.receiverId, message.senderId)

            // 构造元数据
            val meta = ChatProtocol.MediaMessage(
                messageId = message.messageId,
                senderId = message.senderId,
                receiverId = message.receiverId,
                messageType = message.contentType,
                content = message.content,
                fileSize = fileSize,
                checksum = checksum,
                mediaDuration = message.mediaDuration,
                timestamp = message.timestamp
            )

            // WiFi Lock: 后台传输保持高性能
            wifiLockManager.withLock {
                socketManager.sendAtomicTransfer(message.receiverId) { writer ->
                    // FILE_META: 立即 flush，让接收端尽快进入接收状态
                    writer.write(Packet(PacketType.FILE_META, serializeMediaMeta(meta)))

                    // FILE_CHUNK: writeNoFlush，由 buffer 自动 flush，减少 syscall
                    streamFileChunks(file, fileSize, message.messageId) { chunk ->
                        writer.writeNoFlush(Packet(PacketType.FILE_CHUNK, chunk))
                    }
                    // sendAtomicTransfer 会在 block 结束后调用 writer.flush()
                }.getOrThrow()
            }

            updateStatus(message.messageId, SendStatus.Sent)
            Log.d(TAG, "媒体消息已发送: ${message.messageId}, 大小: ${fileSize / 1024}KB")

            Unit
        }.onFailure { error ->
            handleSendError(message.messageId)
            throw error
        }
    }

    suspend fun sendReadReceipt(messageId: String, senderId: String) {
        sendReceiptSafely(senderId) {
            Packet(
                PacketType.READ_RECEIPT, serializePolymorphic(
                    ChatProtocol.MessageRead(messageId, senderId, System.currentTimeMillis())
                )
            )
        }
    }

    suspend fun sendAck(messageId: String, senderId: String) {
        sendReceiptSafely(senderId) {
            Packet(
                PacketType.ACK, serializePolymorphic(
                    ChatProtocol.MessageAck(messageId, senderId, System.currentTimeMillis())
                )
            )
        }
    }

    // ==================== 内部逻辑 ====================

    private suspend fun sendSinglePacket(
        message: MessageEntity,
        packetBuilder: () -> Packet
    ): Result<Unit> = runCatching {
        ensureConnected(message.receiverId, message.senderId)
        socketManager.send(message.receiverId, packetBuilder()).getOrThrow()
        updateStatus(message.messageId, SendStatus.Sent)
    }.onFailure { error ->
        handleSendError(message.messageId)
        throw error
    }

    private suspend fun sendReceiptSafely(receiverId: String, packetBuilder: () -> Packet) {
        withContext(Dispatchers.IO) {
            runCatching {
                socketManager.send(receiverId, packetBuilder()).getOrThrow()
            }.onFailure { Log.e(TAG, "回执发送失败: $receiverId", it) }
        }
    }

    // ==================== 流式文件分片 ====================

    /**
     * 流式读文件，按 chunk 回调
     *
     * - buffer 复用，避免每片 new ByteArray
     * - 进度按 PROGRESS_REPORT_INTERVAL 节流
     */
    private suspend inline fun streamFileChunks(
        file: File,
        fileSize: Long,
        messageId: String,
        crossinline onChunk: (ByteArray) -> Unit
    ) = withContext(Dispatchers.IO) {
        val buffer = ByteArray(TransferConfig.FILE_CHUNK_SIZE)
        var totalSent = 0L
        var lastReportedAt = 0L

        FileInputStream(file).use { fis ->
            while (true) {
                val bytesRead = fis.read(buffer)
                if (bytesRead <= 0) break

                val chunk = buffer.copyOf(bytesRead)
                onChunk(chunk)

                totalSent += bytesRead

                // 进度节流
                if (fileSize > 0 && totalSent - lastReportedAt >= TransferConfig.PROGRESS_REPORT_INTERVAL) {
                    lastReportedAt = totalSent
                    updateProgress(messageId, totalSent, fileSize)
                }
            }

            // 最后上报100%的进度
            if (totalSent > lastReportedAt) {
                updateProgress(messageId, totalSent, fileSize)
            }
        }
    }

    private suspend fun updateProgress(id: String, sent: Long, total: Long) {
        val percent = (sent.toDouble() / total * 100).toInt()
        Log.d(TAG, "发送 [$id]: $percent%")
        messageDao.updateSentBytes(id, sent)
    }

    // ==================== 连接管理 ====================

    private suspend fun ensureConnected(targetUserId: String, myUserId: String) {
        if (socketManager.isConnected(targetUserId)) return

        val info = connectionInfoDao.getConnectionsByUserId(targetUserId)
            .firstOrNull { it.ipAddress != null && it.port != null }
            ?: throw ConnectionException("未找到有效连接信息，对方可能不在同一网络")

        socketManager.connect(
            userId = targetUserId,
            host = info.ipAddress!!,
            port = info.port!!,
            myUserId = myUserId
        ).getOrElse { throw ConnectionException("连接失败，对方可能已离线", it) }
    }

    // ==================== 序列化 ====================

    private fun serializePolymorphic(protocol: ChatProtocol): ByteArray =
        json.encodeToString<ChatProtocol>(protocol).toByteArray(Charsets.UTF_8)

    private fun serializeMediaMeta(meta: ChatProtocol.MediaMessage): ByteArray =
        json.encodeToString(meta).toByteArray(Charsets.UTF_8)

    // ==================== 工具 ====================

    private suspend fun updateStatus(messageId: String, status: SendStatus) {
        messageDao.updateSendStatus(messageId, status)
    }

    private suspend fun handleSendError(messageId: String) {
        updateStatus(messageId, SendStatus.Failed)
    }
}