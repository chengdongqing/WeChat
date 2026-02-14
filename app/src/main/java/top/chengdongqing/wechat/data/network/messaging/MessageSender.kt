package top.chengdongqing.wechat.data.network.messaging

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.SendStatus
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.socket.SocketManager
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
    private val socketManager: SocketManager,
    private val connectionInfoDao: ConnectionInfoDao,
    private val messageDao: MessageDao,
    private val json: Json
) {

    private companion object {
        private const val CHUNK_SIZE = 64 * 1024  // 64KB

        const val TAG = "MessageSender"
    }

    /**
     * 发送文本消息
     */
    suspend fun sendTextMessage(message: MessageEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. 确保连接存在，不存在则尝试建立
                ensureConnected(message.receiverId, message.senderId)

                // 2. 构造协议消息
                val protocol = ChatProtocol.TextMessage(
                    messageId = message.messageId,
                    senderId = message.senderId,
                    receiverId = message.receiverId,
                    content = message.content,
                    timestamp = message.timestamp
                )

                // 3. 序列化
                val data = json.encodeToString<ChatProtocol>(protocol).toByteArray(Charsets.UTF_8)

                // 4. 发送
                socketManager.send(message.receiverId, data).getOrThrow()

                // 5. 更新发送状态
                messageDao.updateSendStatus(message.messageId, SendStatus.Sent)

                Log.d(TAG, "✅ 消息已发送: ${message.messageId}")

                Unit
            }.onFailure { error ->
                Log.e(TAG, "发送失败: ${message.messageId}", error)
                messageDao.updateSendStatus(message.messageId, SendStatus.Failed)
            }
        }

    /**
     * 发送媒体消息
     * 先发 JSON 元数据，再用 FileInputStream 分块发二进制
     */
    suspend fun sendMediaMessage(message: MessageEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. 确保连接存在，不存在则尝试建立
                ensureConnected(message.receiverId, message.senderId)

                val file = File(message.localPath ?: throw Exception("文件路径为空"))
                if (!file.exists()) throw Exception("文件不存在: ${message.localPath}")

                val startBytes = message.sentBytes
                var totalSent = startBytes
                val buffer = ByteArray(CHUNK_SIZE)

                // 1. 发 FileHeader（JSON）
                val header = ChatProtocol.FileHeader(
                    messageId = message.messageId,
                    senderId = message.senderId,
                    receiverId = message.receiverId,
                    messageType = message.contentType,
                    content = message.content,
                    fileSize = file.length(),
                    mediaDuration = message.mediaDuration,
                    resumeFrom = startBytes,
                    timestamp = message.timestamp
                )
                val headerData = json.encodeToString<ChatProtocol>(header).toByteArray()
                socketManager.send(message.receiverId, headerData).getOrThrow()

                // 2. FileInputStream 边读边发（不会 OOM）
                FileInputStream(file).use { fis ->
                    if (startBytes > 0) fis.skip(startBytes)

                    var bytesRead = 0
                    while (
                        isActive &&  // 协程取消时退出循环
                        fis.read(buffer).also { bytesRead = it } != -1
                    ) {
                        // 让协程框架有机会响应取消信号
                        yield()

                        socketManager.sendBinary(
                            userId = message.receiverId,
                            messageId = message.messageId,
                            data = buffer,
                            offset = 0,
                            length = bytesRead
                        ).getOrThrow()

                        totalSent += bytesRead

                        if (totalSent % (1024 * 1024) == 0L) {
                            messageDao.updateSentBytes(message.messageId, totalSent)
                        }
                    }
                    // 确保最终进度写入
                    messageDao.updateSentBytes(message.messageId, totalSent)
                }

                // 3. 发 FileEnd（JSON）
                if (isActive) {
                    val end = ChatProtocol.FileEnd(
                        messageId = message.messageId,
                        senderId = message.senderId
                    )
                    val endData = json.encodeToString<ChatProtocol>(end).toByteArray()
                    socketManager.send(message.receiverId, endData).getOrThrow()

                    messageDao.updateSendStatus(message.messageId, SendStatus.Sent)
                    Log.d(TAG, "✅ 文件发送完成: ${message.messageId}")
                } else {
                    // 取消：保留 sentBytes，下次可以断点续传
                    Log.d(TAG, "发送已取消: ${message.messageId}, 已发 $totalSent 字节")
                }

                Unit
            }.onFailure { error ->
                if (error is CancellationException) throw error  // ✅ 取消异常必须重新抛出
                Log.e(TAG, "发送媒体失败: ${message.messageId}", error)
                messageDao.updateSendStatus(message.messageId, SendStatus.Failed)
            }
        }

    /**
     * 确保已连接，没有则自动重连
     */
    private suspend fun ensureConnected(userId: String, myUserId: String) {
        // 已连接则直接返回
        if (socketManager.isConnected(userId)) return

        Log.d(TAG, "未找到连接，尝试建立: $userId")

        // 从数据库查找连接信息
        val connectionInfo = connectionInfoDao
            .getConnectionsByUserId(userId)
            .firstOrNull { it.ipAddress != null && it.port != null }
            ?: throw Exception("未找到 $userId 的连接信息，对方可能不在同一网络")

        // 建立连接
        socketManager.connect(
            userId = userId,
            host = connectionInfo.ipAddress!!,
            port = connectionInfo.port!!,
            myUserId = myUserId
        ).getOrElse {
            throw Exception("连接失败，对方可能已离线")
        }
    }

    /**
     * 发送消息已读回执
     */
    suspend fun sendReadReceipt(messageId: String, senderId: String) {
        withContext(Dispatchers.IO) {
            try {
                val protocol = ChatProtocol.MessageRead(
                    messageId = messageId,
                    senderId = senderId,
                    timestamp = System.currentTimeMillis()
                )

                val data = json.encodeToString<ChatProtocol>(protocol).toByteArray()
                socketManager.send(senderId, data)

            } catch (e: Exception) {
                Log.e(TAG, "发送已读回执失败", e)
            }
        }
    }

    /**
     * 发送消息确认（ACK）
     */
    suspend fun sendAck(messageId: String, senderId: String) {
        withContext(Dispatchers.IO) {
            try {
                val protocol = ChatProtocol.MessageAck(
                    messageId = messageId,
                    senderId = senderId,
                    timestamp = System.currentTimeMillis()
                )

                val data = json.encodeToString<ChatProtocol>(protocol).toByteArray()
                socketManager.send(senderId, data)

            } catch (e: Exception) {
                Log.e(TAG, "发送ACK失败", e)
            }
        }
    }
}