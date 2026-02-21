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
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketType
import top.chengdongqing.wechat.data.network.socket.SocketClient
import top.chengdongqing.wechat.data.network.socket.SocketServer
import top.chengdongqing.wechat.data.network.transfer.WifiLockManager
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息发送器
 *
 * 媒体发送流程：流式计算 MD5 → 发 FILE_META（含 checksum）→ 分片发 FILE_CHUNK。
 * META 必须先于 CHUNK 到达接收端，且需要携带完整 checksum，因此两次读取不可避免。
 * LAN 场景下磁盘顺序读极快，额外一次读取开销可忽略。
 *
 * 性能设计：
 * - FILE_CHUNK 256KB，减少包头开销和 syscall 次数
 * - writeNoFlush 批量写入，由 BufferedOutputStream 攒满后统一推送
 * - WifiLock 保持后台传输时 WiFi 高性能模式
 * - 进度上报按 PROGRESS_REPORT_INTERVAL 节流
 */
@Singleton
class MessageSender @Inject constructor(
    private val socketClient: SocketClient,
    private val socketServer: SocketServer,
    private val connectionInfoDao: ConnectionInfoDao,
    private val messageDao: MessageDao,
    private val wifiLockManager: WifiLockManager,
    private val json: Json
) {
    private companion object {
        const val TAG = "MessageSender"
    }

    // ==================== 发送接口 ====================

    /** 发送文本消息 */
    suspend fun sendTextMessage(message: MessageEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            sendSinglePacket(message) {
                Packet(
                    PacketType.TEXT,
                    serializePolymorphic(
                        ChatProtocol.TextMessage(
                            messageId = message.messageId,
                            senderId = message.senderId,
                            receiverId = message.receiverId,
                            messageType = message.contentType,
                            content = message.content,
                            timestamp = message.timestamp
                        )
                    )
                )
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
                val fileSize = file.length()
                val checksum = file.toMD5Hex()
                Log.d(TAG, "MD5 计算完成 [${message.messageId}]: $checksum")

                ensureOutboundConnected(message.receiverId, message.senderId)

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

                wifiLockManager.withTransferLock {
                    socketClient.sendAtomicTransfer(message.receiverId) { writer ->
                        // FILE_META 立即 flush，让接收端尽快进入接收状态
                        writer.write(Packet(PacketType.FILE_META, serializeMediaMeta(meta)))
                        // FILE_CHUNK writeNoFlush，由 buffer 自动 flush 减少 syscall
                        streamFileChunks(file, fileSize, message.messageId) { chunk ->
                            writer.writeNoFlush(Packet(PacketType.FILE_CHUNK, chunk))
                        }
                        // sendAtomicTransfer 在 block 结束后统一 flush
                    }.getOrThrow()
                }

                updateStatus(message.messageId, SendStatus.Sent)
                Log.d(TAG, "媒体消息已发送: [${message.messageId}] ${fileSize / 1024}KB")

                Unit
            }.onFailure {
                handleSendError(message.messageId)
                throw it
            }
        }

    /** 发送送达回执 */
    suspend fun sendAck(messageId: String, senderId: String) {
        sendReceiptSafely(senderId) {
            Packet(
                PacketType.ACK,
                serializePolymorphic(
                    ChatProtocol.MessageAck(messageId, senderId, System.currentTimeMillis())
                )
            )
        }
    }

    /** 发送已读回执 */
    suspend fun sendReadReceipt(messageId: String, senderId: String) {
        sendReceiptSafely(senderId) {
            Packet(
                PacketType.READ_RECEIPT,
                serializePolymorphic(
                    ChatProtocol.MessageRead(messageId, senderId, System.currentTimeMillis())
                )
            )
        }
    }

    // ==================== 连接管理 ====================

    /**
     * 确保与目标用户有可用连接（文本/回执用）
     *
     * 出站或入站连接有其一即可，优先复用已有连接。
     * 若两者均无，从数据库取地址主动建出站连接。
     */
    suspend fun ensureConnected(targetUserId: String, myUserId: String) {
        if (socketClient.isConnected(targetUserId) || socketServer.isClientConnected(targetUserId)) return
        connectFromDb(targetUserId, myUserId)
    }

    /**
     * 确保与目标用户有出站连接（媒体传输专用）
     *
     * [SocketClient.sendAtomicTransfer] 需要出站连接的 Mutex，不能走入站连接降级。
     */
    suspend fun ensureOutboundConnected(targetUserId: String, myUserId: String) {
        if (socketClient.isConnected(targetUserId)) return
        connectFromDb(targetUserId, myUserId)
    }

    /**
     * 从数据库取连接信息并建立出站连接
     *
     * 数据库无记录或连接失败均抛 [ConnectionException]，由调用方将消息置为 Failed。
     */
    private suspend fun connectFromDb(targetUserId: String, myUserId: String) {
        val info = connectionInfoDao.getConnectionsByUserId(targetUserId)
            .firstOrNull { it.ipAddress != null && it.port != null }
            ?: throw ConnectionException("未找到连接信息: $targetUserId")

        socketClient.connect(
            userId = targetUserId,
            host = info.ipAddress!!,
            port = info.port!!,
            myUserId = myUserId
        ).getOrElse { throw ConnectionException("连接失败: $targetUserId", it) }
    }

    // ==================== 内部逻辑 ====================

    /** 发送单个 Packet，失败时将消息状态置为 Failed */
    private suspend fun sendSinglePacket(
        message: MessageEntity,
        packetBuilder: () -> Packet
    ): Result<Unit> = runCatching {
        ensureConnected(message.receiverId, message.senderId)
        sendPacket(message.receiverId, packetBuilder()).getOrThrow()
        updateStatus(message.messageId, SendStatus.Sent)
    }.onFailure {
        handleSendError(message.messageId)
        throw it
    }

    /** 发送回执类 Packet，失败只打日志，不影响主流程 */
    private suspend fun sendReceiptSafely(receiverId: String, packetBuilder: () -> Packet) {
        withContext(Dispatchers.IO) {
            runCatching {
                sendPacket(receiverId, packetBuilder()).getOrThrow()
            }.onFailure { Log.e(TAG, "回执发送失败: $receiverId", it) }
        }
    }

    /**
     * 向指定用户发送单个 Packet
     *
     * 优先走出站连接，无则降级走入站连接，两者均无则失败。
     */
    private suspend fun sendPacket(receiverId: String, packet: Packet): Result<Unit> = when {
        socketClient.isConnected(receiverId) -> socketClient.send(receiverId, packet)
        socketServer.isClientConnected(receiverId) -> socketServer.sendToClient(receiverId, packet)
        else -> Result.failure(IllegalStateException("无可用连接: $receiverId"))
    }

    /**
     * 流式读文件，按 chunk 回调
     *
     * buffer 复用避免频繁 GC，进度按 [TransferConfig.PROGRESS_REPORT_INTERVAL] 节流上报。
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
                onChunk(buffer.copyOf(bytesRead))
                totalSent += bytesRead
                if (fileSize > 0 && totalSent - lastReportedAt >= TransferConfig.PROGRESS_REPORT_INTERVAL) {
                    lastReportedAt = totalSent
                    updateProgress(messageId, totalSent, fileSize)
                }
            }
            if (totalSent > lastReportedAt) updateProgress(messageId, totalSent, fileSize)
        }
    }

    // ==================== 工具 ====================

    private suspend fun updateProgress(id: String, sent: Long, total: Long) {
        val percent = (sent.toDouble() / total * 100).toInt()
        Log.d(TAG, "发送进度 [$id]: $percent%")
        messageDao.updateSentBytes(id, sent)
    }

    private suspend fun updateStatus(messageId: String, status: SendStatus) =
        messageDao.updateSendStatus(messageId, status)

    private suspend fun handleSendError(messageId: String) =
        updateStatus(messageId, SendStatus.Failed)

    /** 多态序列化，保留 type discriminator 供接收端反序列化子类 */
    private fun serializePolymorphic(protocol: ChatProtocol): ByteArray =
        json.encodeToString<ChatProtocol>(protocol).toByteArray(Charsets.UTF_8)

    /** 媒体元数据序列化，无需多态 discriminator */
    private fun serializeMediaMeta(meta: ChatProtocol.MediaMessage): ByteArray =
        json.encodeToString(meta).toByteArray(Charsets.UTF_8)
}

class ConnectionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)