package top.chengdongqing.wechat.data.network.messaging

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.SendStatus
import top.chengdongqing.wechat.data.network.config.TransferConfig
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
     * 发送媒体消息（流式，内存安全，带宽优化）
     *
     * 完整流程:
     * 1. 确保连接
     * 2. 获取 WiFi Lock（后台不降速）
     * 3. transferMutex 原子区内:
     *    - 写 FILE_META（flush）
     *    - 流式读文件 → writeNoFlush FILE_CHUNK（buffer 满自动 flush）
     *    - sendAtomicTransfer 尾部统一 flush 残余
     * 4. 释放 WiFi Lock
     */
    suspend fun sendMediaMessage(
        message: MessageEntity,
        file: File
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            ensureConnected(message.receiverId, message.senderId)

            val fileSize = file.length()
            val meta = ChatProtocol.MediaMessage(
                messageId = message.messageId,
                senderId = message.senderId,
                receiverId = message.receiverId,
                messageType = message.contentType,
                content = message.content,
                fileSize = fileSize,
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
            handleSendError(message.messageId, "发送媒体失败", error)
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
        handleSendError(message.messageId, "发送失败", error)
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
    private inline fun streamFileChunks(
        file: File,
        fileSize: Long,
        messageId: String,
        onChunk: (ByteArray) -> Unit
    ) {
        val buffer = ByteArray(TransferConfig.FILE_CHUNK_SIZE)
        var totalSent = 0L
        var lastReportedAt = 0L

        FileInputStream(file).buffered(TransferConfig.FILE_READ_BUFFER).use { fis ->
            while (true) {
                val bytesRead = fis.read(buffer)
                if (bytesRead == -1) break

                val chunk = if (bytesRead == buffer.size) buffer else buffer.copyOf(bytesRead)
                onChunk(chunk)

                totalSent += bytesRead

                // 进度节流: 每 1MB 报告一次
                if (fileSize > 0 && totalSent - lastReportedAt >= TransferConfig.PROGRESS_REPORT_INTERVAL) {
                    lastReportedAt = totalSent
                    Log.d(TAG, "发送 [$messageId]: ${(totalSent * 100) / fileSize}%")
                }
            }
        }
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

    private suspend fun handleSendError(messageId: String, msg: String, error: Throwable) {
        Log.e(TAG, "$msg: $messageId", error)
        updateStatus(messageId, SendStatus.Failed)
    }

    private class ConnectionException(
        message: String, cause: Throwable? = null
    ) : Exception(message, cause)
}