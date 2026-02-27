package top.chengdongqing.wechat.data.network.messaging

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.util.toMD5Hex
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.SendError
import top.chengdongqing.wechat.data.database.entity.SendStatus
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.connection.ConnectionException
import top.chengdongqing.wechat.data.network.connection.ConnectionManager
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketType
import top.chengdongqing.wechat.data.network.protocol.ReceiptType
import top.chengdongqing.wechat.data.network.socket.SocketClient
import top.chengdongqing.wechat.data.network.transfer.TransferManager
import top.chengdongqing.wechat.data.network.transfer.WifiLockManager
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * 消息发送器
 */
@Singleton
class MessageSender @Inject constructor(
    private val weDatabase: WeDatabase,
    private val socketClient: SocketClient,
    private val connectionManager: ConnectionManager,
    private val connectionInfoDao: ConnectionInfoDao,
    private val messageDao: MessageDao,
    private val chatSessionDao: ChatSessionDao,
    private val wifiLockManager: WifiLockManager,
    private val transferManager: TransferManager,
    private val json: Json
) {
    private companion object {
        const val TAG = "MessageSender"
    }

    /**
     * 发送文本消息
     */
    suspend fun sendTextMessage(message: MessageEntity): Result<Unit> {
        val packet = Packet(
            PacketType.TEXT,
            serializePolymorphic(
                ChatProtocol.TextMessage(
                    messageId = message.id,
                    senderId = message.senderId,
                    receiverId = message.receiverId,
                    messageType = message.contentType,
                    content = message.content,
                    timestamp = message.timestamp
                )
            )
        )

        return runCatching {
            ensureConnected(message.receiverId, message.senderId)
            connectionManager.send(message.receiverId, packet)
            updateStatus(
                messageId = message.id,
                sessionId = message.receiverId,
                status = SendStatus.Sent
            )
        }.onFailure { e ->
            handleSendError(message.id, message.receiverId, e)
            throw e
        }
    }

    /**
     * 发送媒体消息
     *
     * 流程：算 MD5 → 确保连接 → WiFi Lock → 原子发送（META + CHUNK）
     */
    suspend fun sendMediaMessage(message: MessageEntity, file: File): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 确保已连接
                ensureConnected(message.receiverId, message.senderId)

                // 获取文件大小
                val fileSize = file.length()
                // 计算哈希值
                val checksum = file.toMD5Hex()

                // 构建消息元数据
                val meta = ChatProtocol.MediaMessage(
                    messageId = message.id,
                    senderId = message.senderId,
                    receiverId = message.receiverId,
                    messageType = message.contentType,
                    content = message.content,
                    fileSize = fileSize,
                    checksum = checksum,
                    mediaDuration = message.mediaDuration,
                    timestamp = message.timestamp
                )

                // 获取 Wi-Fi 锁，避免在后台传输时被系统限制性能
                wifiLockManager.withTransferLock {
                    connectionManager.sendAtomicTransfer(message.receiverId) { writer ->
                        // 发送消息元数据；FILE_META 立即 flush，让接收端尽快进入接收状态
                        writer.write(Packet(PacketType.FILE_META, serializeMediaMeta(meta)))
                        // 分片分送文件；FILE_CHUNK writeNoFlush，由 buffer 自动 flush 减少 syscall
                        streamFileChunks(file, fileSize, message.id) { chunk ->
                            writer.writeNoFlush(Packet(PacketType.FILE_CHUNK, chunk))
                        }
                    }.getOrThrow()
                }

                // 更新消息的发送状态
                updateStatus(
                    messageId = message.id,
                    sessionId = message.receiverId,
                    status = SendStatus.Sent
                )
            }.onFailure { e ->
                handleSendError(message.id, message.receiverId, e)
                throw e
            }
        }

    /**
     * 发送回执消息
     */
    suspend fun sendReceipt(messageId: String, senderId: String, type: ReceiptType) {
        val packet = Packet(
            PacketType.RECEIPT,
            serializePolymorphic(
                ChatProtocol.MessageReceipt(
                    messageId = messageId,
                    senderId = senderId,
                    receiptType = type,
                    timestamp = System.currentTimeMillis()
                )
            )
        )

        runCatching {
            connectionManager.send(senderId, packet)
        }.onFailure { e ->
            Log.e(TAG, "回执发送失败: $senderId", e)
        }
    }

    /**
     * 确保与目标用户有可用连接
     */
    suspend fun ensureConnected(targetUserId: String, myUserId: String) {
        if (connectionManager.isConnected(targetUserId)) return
        connectFromDb(targetUserId, myUserId)
    }

    /**
     * 从数据库取连接信息并建立出站连接
     */
    private suspend fun connectFromDb(targetUserId: String, myUserId: String) {
        val info = connectionInfoDao.getById(targetUserId)
            .takeIf { it?.ipAddress != null && it.port != null }
            ?: throw ConnectionException(
                "未找到连接信息: $targetUserId",
                SendError.ConnectionFailed
            )

        socketClient.connect(
            userId = targetUserId,
            host = info.ipAddress!!,
            port = info.port!!,
            myUserId = myUserId
        ).getOrElse {
            throw ConnectionException("连接失败: $targetUserId", SendError.ConnectionFailed)
        }
    }

    /**
     * 流式读文件，按 chunk 回调
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

                // 判断是否要取消传输
                if (transferManager.isCancelled(messageId)) {
                    transferManager.remove(messageId)
                    throw CancellationException("已取消发送")
                }

                onChunk(buffer.copyOf(bytesRead))
                totalSent += bytesRead
                if (fileSize > 0 && totalSent - lastReportedAt >= TransferConfig.PROGRESS_REPORT_INTERVAL) {
                    lastReportedAt = totalSent
                    updateProgress(messageId, totalSent)
                }
            }
            if (totalSent > lastReportedAt) updateProgress(messageId, totalSent)
        }
    }

    /**
     * 更新文件发送的进度
     */
    private suspend fun updateProgress(messageId: String, sentBytes: Long) {
        messageDao.update(messageId) { message ->
            message.copy(sentBytes = sentBytes)
        }
    }

    /**
     * 更新发送状态
     */
    private suspend fun updateStatus(messageId: String, sessionId: String, status: SendStatus) {
        weDatabase.withTransaction {
            messageDao.update(messageId) { message ->
                message.copy(sendStatus = status)
            }
            // 更新会话状态
            if (status == SendStatus.Sent) {
                chatSessionDao.update(sessionId) { session ->
                    session.copy(isSending = false)
                }
            }
        }
    }

    /**
     * 处理发送失败
     */
    private suspend fun handleSendError(
        messageId: String,
        receiverId: String,
        error: Throwable
    ) {
        val failReason = if (error is ConnectionException) {
            error.failReason
        } else {
            if (error is CancellationException) {
                SendError.Cancelled
            } else {
                SendError.Unknown
            }
        }

        weDatabase.withTransaction {
            // 更新状态
            messageDao.update(messageId) { message ->
                message.copy(
                    sendStatus = SendStatus.Failed,
                    failReason = failReason
                )
            }
            // 更新会话状态
            chatSessionDao.update(receiverId) { session ->
                session.copy(isSending = false)
            }
        }
        // 标记为离线
        connectionInfoDao.markOffline(receiverId)
    }

    /**
     * 多态序列化
     */
    private fun serializePolymorphic(protocol: ChatProtocol): ByteArray =
        json.encodeToString<ChatProtocol>(protocol).toByteArray(Charsets.UTF_8)

    /**
     * 媒体元数据序列化
     */
    private fun serializeMediaMeta(meta: ChatProtocol.MediaMessage): ByteArray =
        json.encodeToString(meta).toByteArray(Charsets.UTF_8)
}