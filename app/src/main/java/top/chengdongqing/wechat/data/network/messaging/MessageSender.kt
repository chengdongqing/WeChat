package top.chengdongqing.wechat.data.network.messaging

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.ConnectionType
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.SendStatus
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.socket.SocketManager
import javax.inject.Inject
import javax.inject.Singleton

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
     * 发送媒体消息（需要先传文件）
     */
    suspend fun sendMediaMessage(
        message: MessageEntity,
        fileData: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            getActiveConnection(message.receiverId)
                ?: throw Exception("对方不在线")

            // 1. 先发送媒体消息元数据
            val protocol = ChatProtocol.MediaMessage(
                messageId = message.messageId,
                senderId = message.senderId,
                receiverId = message.receiverId,
                messageType = message.contentType,
                content = message.content,
                mediaSize = fileData.size.toLong(),
                mediaDuration = message.mediaDuration,
                timestamp = message.timestamp
            )

            val metaData = json.encodeToString<ChatProtocol>(protocol).toByteArray()
            socketManager.send(message.receiverId, metaData).getOrThrow()

            // 2. 再发送文件数据（分片传输）
            sendFileData(message.receiverId, fileData)

            // 3. 更新状态
            messageDao.updateSendStatus(message.messageId, SendStatus.Sent)

            Log.d(TAG, "✅ 媒体消息已发送: ${message.messageId}")

            Unit
        }.onFailure { error ->
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

    /**
     * 获取活跃的连接
     */
    private suspend fun getActiveConnection(userId: String): top.chengdongqing.wechat.data.database.entity.ConnectionInfoEntity? {
        // 优先使用 WiFi LAN
        val connections = connectionInfoDao.getConnectionsByUserId(userId)
        return connections.firstOrNull {
            it.connectionType == ConnectionType.WiFiLan && it.isOnline
        }
    }

    /**
     * 分片发送文件数据
     */
    private suspend fun sendFileData(userId: String, fileData: ByteArray) {
        val chunkSize = 64 * 1024  // 64KB 每片
        var offset = 0

        while (offset < fileData.size) {
            val remaining = fileData.size - offset
            val size = minOf(chunkSize, remaining)
            val chunk = fileData.copyOfRange(offset, offset + size)

            socketManager.send(userId, chunk).getOrThrow()

            offset += size

            Log.d(TAG, "文件传输进度: ${offset * 100 / fileData.size}%")
        }
    }
}