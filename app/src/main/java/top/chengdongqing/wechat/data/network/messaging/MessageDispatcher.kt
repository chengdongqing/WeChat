package top.chengdongqing.wechat.data.network.messaging

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.database.entity.MessageType
import top.chengdongqing.wechat.data.database.entity.SendStatus
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.SocketFrame
import top.chengdongqing.wechat.features.chat.data.mapper.MediaContent
import top.chengdongqing.wechat.features.chat.data.mapper.toDomain
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息分发器
 */
@Singleton
class MessageDispatcher @Inject constructor(
    private val messageDao: MessageDao,
    private val connectionInfoDao: ConnectionInfoDao,
    private val messageSender: MessageSender,
    private val chatSessionUpdater: ChatSessionUpdater,
    private val json: Json,
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "MessageDispatcher"
    }

    private val fileReceiveBuffers = ConcurrentHashMap<String, FileReceiveBuffer>()

    // ✅ 聊天消息（已存库）
    private val _incomingMessageFlow = MutableSharedFlow<ChatMessage>(
        replay = 0,
        extraBufferCapacity = 64  // 防止背压丢消息
    )
    val incomingMessageFlow: SharedFlow<ChatMessage> = _incomingMessageFlow.asSharedFlow()

    // ✅ 信令消息（不存库）
    private val _signalingFlow = MutableSharedFlow<SignalingMessage>(
        replay = 0,
        extraBufferCapacity = 32
    )
    val signalingFlow: SharedFlow<SignalingMessage> = _signalingFlow.asSharedFlow()

    // ==================== 分发入口 ====================

    suspend fun dispatch(protocol: ChatProtocol) = withContext(Dispatchers.IO) {
        when (protocol) {
            is ChatProtocol.TextMessage -> handleChatMessage(protocol)
            is ChatProtocol.FileHeader -> handleFileHeader(protocol)
            is ChatProtocol.FileEnd -> handleFileEnd(protocol)
            is ChatProtocol.MessageAck -> handleAck(protocol)
            is ChatProtocol.MessageRead -> handleRead(protocol)
            is ChatProtocol.Signaling -> handleSignaling(protocol)
            is ChatProtocol.Heartbeat -> handleHeartbeat(protocol)
        }
    }

    private suspend fun handleFileHeader(protocol: ChatProtocol.FileHeader) =
        withContext(Dispatchers.IO) {
            if (messageDao.exists(protocol.messageId)) return@withContext

            // 存数据库占位
            val entity = buildEntityFromHeader(protocol)
            messageDao.insert(entity)
            chatSessionUpdater.update(entity)
            messageSender.sendAck(protocol.messageId, protocol.senderId)

            // 创建文件接收缓冲
            val subDir = when (protocol.messageType) {
                MessageType.Image -> "images"
                MessageType.Video -> "videos"
                MessageType.Voice -> "voices"
                MessageType.File -> "files"
                else -> "media"
            }
            val dir = File(context.filesDir, subDir).also { it.mkdirs() }

            val data = json.decodeFromString<MediaContent>(protocol.content)
            val finalFile = File(dir, "${protocol.messageId}_${data.filename}")
            val tmpFile = File(dir, "${protocol.messageId}.tmp")  // 先写临时文件

            fileReceiveBuffers[protocol.messageId] = FileReceiveBuffer(
                messageId = protocol.messageId,
                tmpFile = tmpFile,          // 写入临时文件
                finalFile = finalFile,      // 完成后重命名目标
                outputStream = FileOutputStream(tmpFile, true),  // append 支持断点续传
                totalSize = protocol.fileSize,
                receivedSize = tmpFile.length()  // 断点续传从已有大小继续
            )

            Log.d(TAG, "开始接收文件: ${protocol.messageId}")
        }

    private suspend fun handleFileEnd(protocol: ChatProtocol.FileEnd) =
        withContext(Dispatchers.IO) {
            val buffer = fileReceiveBuffers[protocol.messageId] ?: return@withContext
            if (buffer.receivedSize < buffer.totalSize) {
                Log.w(TAG, "FileEnd 到达但字节未收齐，等待剩余数据")
                return@withContext
            }
            fileReceiveBuffers.remove(protocol.messageId) ?: return@withContext

            // 关闭文件流
            buffer.outputStream.flush()
            buffer.outputStream.close()

            // 清理旧文件
            if (buffer.finalFile.exists()) {
                buffer.finalFile.delete()
            }

            // 重命名临时文件
            if (!buffer.tmpFile.renameTo(buffer.finalFile)) {
                Log.e(TAG, "文件重命名失败: ${buffer.tmpFile}")
                buffer.tmpFile.delete()  // 清理残留
                messageDao.updateSendStatus(protocol.messageId, SendStatus.Failed)
                return@withContext
            }

            // 更新本地路径
            messageDao.updateLocalPath(protocol.messageId, buffer.finalFile.absolutePath)

            // 推送到 UI
            val updated = messageDao.getByMessageId(protocol.messageId) ?: return@withContext
            _incomingMessageFlow.emit(updated.toDomain(json))

            Log.d(TAG, "✅ 文件接收完成: ${protocol.messageId} → ${buffer.finalFile.absolutePath}")
        }

    // ==================== 聊天消息 ====================

    private suspend fun handleChatMessage(protocol: ChatProtocol) {
        try {
            // 去重
            if (messageDao.exists(protocol.messageId)) {
                Log.d(TAG, "消息已存在，跳过: ${protocol.messageId}")
                messageSender.sendAck(protocol.messageId, protocol.senderId)
                return
            }

            // 解密（预留扩展点）
            val decrypted = decrypt(protocol)

            // 持久化
            val entity = buildEntity(decrypted)
            messageDao.insert(entity)

            // 更新会话
            chatSessionUpdater.update(entity)

            // 回复 ACK
            messageSender.sendAck(protocol.messageId, protocol.senderId)

            // 推入 MessageFlow
            _incomingMessageFlow.emit(entity.toDomain(json))

            Log.d(TAG, "✅ 消息已处理: ${protocol.messageId}")
        } catch (e: Exception) {
            Log.e(TAG, "处理聊天消息失败", e)
        }
    }

    suspend fun dispatchBinary(frame: SocketFrame.BinaryFrame) = withContext(Dispatchers.IO) {
        val buffer = fileReceiveBuffers[frame.messageId] ?: run {
            Log.e(TAG, "❌ 找不到 buffer: ${frame.messageId}")
            return@withContext
        }
        Log.d(TAG, "写入 ${frame.data.size} 字节, 累计: ${buffer.receivedSize}")

        synchronized(buffer.outputStream) {  // 保证串行写入
            buffer.outputStream.write(frame.data)
            buffer.receivedSize += frame.data.size
        }
    }

    // ==================== 回执 ====================

    private suspend fun handleAck(protocol: ChatProtocol.MessageAck) {
        messageDao.updateSendStatus(protocol.messageId, SendStatus.Delivered)
        Log.d(TAG, "消息已送达: ${protocol.messageId}")
    }

    private suspend fun handleRead(protocol: ChatProtocol.MessageRead) {
        messageDao.updateSendStatus(protocol.messageId, SendStatus.Read)
        Log.d(TAG, "消息已读: ${protocol.messageId}")
    }

    // ==================== 信令 ====================

    private suspend fun handleSignaling(protocol: ChatProtocol.Signaling) {

    }

    // ==================== 心跳 ====================

    private suspend fun handleHeartbeat(protocol: ChatProtocol.Heartbeat) {
        // 更新对方在线时间
        connectionInfoDao.markOnline(protocol.senderId, protocol.timestamp)
    }

    // ==================== 工具方法 ====================

    /**
     * 统一解密入口
     */
    private fun decrypt(protocol: ChatProtocol): ChatProtocol {
        // TODO
        return protocol
    }

    private fun buildEntityFromHeader(protocol: ChatProtocol.FileHeader): MessageEntity {
        val now = System.currentTimeMillis()
        println("----protocol:$protocol")

        return MessageEntity(
            messageId = protocol.messageId,
            sessionId = protocol.senderId,
            senderId = protocol.senderId,
            receiverId = protocol.receiverId,
            contentType = protocol.messageType,
            content = protocol.content,
            fileSize = protocol.fileSize,
            mediaDuration = protocol.mediaDuration,
            timestamp = protocol.timestamp,
            sendStatus = SendStatus.Delivered,
            isFromMe = false,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun buildEntity(protocol: ChatProtocol): MessageEntity {
        val now = System.currentTimeMillis()
        return when (protocol) {
            is ChatProtocol.TextMessage -> MessageEntity(
                messageId = protocol.messageId,
                sessionId = protocol.senderId,
                senderId = protocol.senderId,
                receiverId = protocol.receiverId,
                contentType = MessageType.Text,
                content = protocol.content,
                timestamp = protocol.timestamp,
                sendStatus = SendStatus.Delivered,
                isFromMe = false,
                createdAt = now,
                updatedAt = now
            )

            else -> throw IllegalArgumentException("不支持的消息类型: ${protocol::class.simpleName}")
        }
    }
}

data class FileReceiveBuffer(
    val messageId: String,
    val tmpFile: File,          // 临时文件
    val finalFile: File,        // 正式文件
    val outputStream: FileOutputStream,
    val totalSize: Long,
    var receivedSize: Long = 0
)