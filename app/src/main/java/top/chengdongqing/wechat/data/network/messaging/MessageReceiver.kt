package top.chengdongqing.wechat.data.network.messaging

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.core.util.toSHA256Hex
import top.chengdongqing.wechat.data.database.dao.MediaFileDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.model.SendError
import top.chengdongqing.wechat.data.model.SendStatus
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.connection.ChatTransportManager
import top.chengdongqing.wechat.data.network.connection.ConnectionEvent
import top.chengdongqing.wechat.data.network.connection.ConnectionMode
import top.chengdongqing.wechat.data.network.connection.PeerConnection
import top.chengdongqing.wechat.data.network.model.ChatProtocol
import top.chengdongqing.wechat.data.network.model.FileAckStatus
import top.chengdongqing.wechat.data.network.model.FileMetaAck
import top.chengdongqing.wechat.data.network.model.Packet
import top.chengdongqing.wechat.data.network.model.PacketType
import top.chengdongqing.wechat.data.network.model.ReceiptType
import top.chengdongqing.wechat.data.network.transfer.TransferManager
import top.chengdongqing.wechat.data.session.FileReferenceManager
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息接收器
 */
@Singleton
class MessageReceiver @Inject constructor(
    private val transport: ChatTransportManager,
    private val messageDispatcher: MessageDispatcher,
    private val permissionChecker: MessagePermissionChecker,
    private val messageSender: MessageSender,
    private val profileRepository: ProfileRepository,
    private val fileReferenceManager: FileReferenceManager,
    private val chunkStorageManager: ChunkStorageManager,
    private val fileAckRegistry: FileAckRegistry,
    private val transferManager: TransferManager,
    private val mediaFileDao: MediaFileDao,
    private val messageDao: MessageDao,
    private val json: Json,
    @param:IoScope private val scope: CoroutineScope,
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "MessageReceiver"
        const val DISK_WRITE_BUFFER = 256 * 1024
    }

    private val receiveContexts = ConcurrentHashMap<String, ReceiveContext>()

    private val myUserId: String
        get() = profileRepository.requireUserId()

    /**
     * 启动监听，订阅新连接并自动开始消费
     */
    fun start() {
        scope.launch {
            transport.connectionEvents.collect { event ->
                when (event) {
                    is ConnectionEvent.Connected -> {
                        startListening(event.conn)
                    }

                    is ConnectionEvent.Disconnected -> {
                        Log.d(TAG, "连接断开: ${event.userId} - ${event.reason}")
                    }
                }
            }
        }
    }

    /**
     * 监听数据
     */
    private fun startListening(conn: PeerConnection) {
        val userId = conn.userId

        scope.launch {
            try {
                for (packet in conn.receiveChannel) {
                    handlePacket(userId, packet)
                }
            } catch (e: Exception) {
                Log.e(TAG, "消费异常: $userId", e)
            } finally {
                cleanupReceiveContext(userId)
                Log.w(TAG, "连接已关闭: $userId")
            }
        }
    }

    /**
     * 按 PacketType 路由到对应处理器
     */
    private suspend fun handlePacket(userId: String, packet: Packet) {
        try {
            when (packet.type) {
                PacketType.FILE_META -> handleFileMeta(userId, packet.body)
                PacketType.FILE_CHUNK -> handleFileChunk(userId, packet.body)

                PacketType.FILE_META_ACK -> handleFileMetaAck(packet.body)
                PacketType.FILE_CANCEL -> handleFileCancel(packet.body)
                PacketType.FILE_PAUSE -> handleFilePause(packet.body)
                PacketType.FILE_RESUME -> handleFileResume(packet.body)

                PacketType.PROFILE_REQUEST -> messageSender.sendProfile(userId)

                else -> {
                    if (packet.body.isNotEmpty()) {
                        handleJsonPacket(userId, packet)
                    } else {
                        Log.w(TAG, "解密后 body 为空，丢弃: $userId")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理 Packet 失败 (userId=$userId, type=${packet.type})", e)
            if (packet.type == PacketType.FILE_CHUNK) {
                cleanupReceiveContext(userId)
            }
        }
    }

    /**
     * 处理JSON包
     */
    private suspend fun handleJsonPacket(userId: String, packet: Packet) {
        val protocol = json.decodeFromString<ChatProtocol>(String(packet.body, Charsets.UTF_8))

        when {
            protocol is ChatProtocol.MessageReceipt -> Unit // 回执消息不判断
            // 判断是否处理此消息（拉黑、不是好友、签名校验等）
            !permissionChecker.checkAndReply(userId, protocol) -> return
        }

        messageDispatcher.dispatch(protocol)
    }

    /**
     * 处理文件元数据包
     */
    private suspend fun handleFileMeta(userId: String, body: ByteArray) {
        // 清理该用户上一个未完成的传输状态
        cleanupReceiveContext(userId)

        val metadata = json.decodeFromString<ChatProtocol.MediaMessage>(
            String(body, Charsets.UTF_8)
        )

        // 权限校验
        if (!permissionChecker.checkAndReply(metadata.senderId, metadata)) {
            return
        }

        if (metadata.fileSize < TransferConfig.CHUNK_TRANSFER_THRESHOLD) {
            handleSmallFileMeta(userId, metadata)
        } else {
            handleLargeFileMeta(userId, metadata)
        }
    }

    /**
     * 小文件：不发 ACK，直接准备接收
     *
     * 创建临时文件 + BufferedOutputStream，等 FILE_CHUNK 追加写入。
     */
    private suspend fun handleSmallFileMeta(userId: String, metadata: ChatProtocol.MediaMessage) =
        withContext(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "recv_${metadata.messageId}.tmp")
            val outputStream = BufferedOutputStream(FileOutputStream(tempFile), DISK_WRITE_BUFFER)

            receiveContexts[userId] = ReceiveContext(
                state = MediaReceiveState(metadata = metadata),
                isLargeFile = false,
                tempFile = tempFile,
                outputStream = outputStream
            )

            Log.d(TAG, "开始接收小文件: messageId=${metadata.messageId}, 大小=${metadata.fileSize}")
        }

    /**
     * 大文件：走协商流程
     *
     * 1. checksum 查重 → 已存在则直接入库，回 AlreadyExists
     * 2. 查分片目录 → 有未完成的分片则回 ResumeFrom(offset)
     * 3. 初始化分片目录，回 ReadyToReceive
     * 4. 创建 Receiving 状态的消息占位（UI 展示进度）
     */
    private suspend fun handleLargeFileMeta(userId: String, metadata: ChatProtocol.MediaMessage) {
        // 哈希查重，已存在则直接成功
        val existingFile = mediaFileDao.getByChecksum(metadata.checksum)
        if (existingFile != null) {
            Log.d(TAG, "文件已存在，跳过传输: ${metadata.messageId}")
            fileReferenceManager.retain(existingFile.localPath, metadata.checksum)

            // 直接分发
            messageDispatcher.dispatchExistingMedia(metadata, existingFile.localPath)

            sendFileMetaAck(userId, metadata.messageId, FileAckStatus.AlreadyExists)
            return
        }

        // 查断点：数据库 sentBytes + 磁盘数据文件
        val existingMessage = messageDao.getById(metadata.messageId)
        val receivedBytes = existingMessage?.sentBytes ?: 0L
        val hasPendingFile = chunkStorageManager.hasPendingTransfer(metadata.messageId)

        if (receivedBytes > 0 && hasPendingFile && receivedBytes < metadata.fileSize) {
            Log.d(
                TAG,
                "断点续传: ${metadata.messageId}, 已接收=$receivedBytes/${metadata.fileSize}"
            )

            // 打开已有文件的写入会话，定位到续传位置
            val writeSession = chunkStorageManager.resumeTransfer(
                metadata.messageId,
                receivedBytes
            )

            if (writeSession != null) {
                receiveContexts[userId] = ReceiveContext(
                    state = MediaReceiveState(
                        metadata = metadata,
                        receivedBytes = receivedBytes,
                        lastReportedAt = receivedBytes,
                        writeSession = writeSession
                    ),
                    isLargeFile = true
                )

                messageDispatcher.updateReceiveProgress(metadata.messageId, receivedBytes)
                sendFileMetaAck(userId, metadata.messageId, FileAckStatus.ResumeFrom, receivedBytes)
                return
            }

            // 打开失败，当作全新传输
            Log.w(TAG, "恢复写入会话失败，重新开始: ${metadata.messageId}")
            chunkStorageManager.cleanup(metadata.messageId)
        }

        // 全新传输：预分配文件 + 打开写入会话
        val writeSession = chunkStorageManager.initTransfer(metadata)

        receiveContexts[userId] = ReceiveContext(
            state = MediaReceiveState(
                metadata = metadata,
                writeSession = writeSession
            ),
            isLargeFile = true
        )

        messageDispatcher.createReceivingMessage(metadata)

        Log.d(TAG, "开始接收大文件: messageId=${metadata.messageId}, 大小=${metadata.fileSize}")

        sendFileMetaAck(userId, metadata.messageId, FileAckStatus.ReadyToReceive)
    }

    /**
     * 处理文件分片包
     *
     * 每个分片写入独立文件，全部到齐后合并->校验->分发
     */
    private suspend fun handleFileChunk(userId: String, chunkData: ByteArray) {
        val ctx = receiveContexts[userId] ?: run {
            Log.w(TAG, "收到 FILE_CHUNK 但无对应 FILE_META (userId=$userId)")
            return
        }

        if (ctx.isLargeFile) {
            handleLargeFileChunk(userId, ctx, chunkData)
        } else {
            handleSmallFileChunk(userId, ctx, chunkData)
        }
    }

    /**
     * 小文件分片：追加写入同一个临时文件
     */
    private suspend fun handleSmallFileChunk(
        userId: String,
        ctx: ReceiveContext,
        chunkData: ByteArray
    ) = withContext(Dispatchers.IO) {
        val state = ctx.state
        val outputStream = ctx.outputStream ?: return@withContext

        outputStream.write(chunkData)
        state.receivedBytes += chunkData.size

        if (state.receivedBytes < state.metadata.fileSize) return@withContext

        // 小文件接收完成
        outputStream.flush()
        outputStream.close()

        val tempFile = ctx.tempFile ?: return@withContext

        // SHA256 校验
        val actualChecksum = tempFile.toSHA256Hex()
        if (actualChecksum != state.metadata.checksum) {
            Log.e(
                TAG,
                "小文件校验失败 [${state.metadata.messageId}]: 期望=${state.metadata.checksum}, 实际=$actualChecksum"
            )
            tempFile.delete()
            receiveContexts.remove(userId)
            return@withContext
        }

        Log.d(TAG, "小文件接收完成: ${state.metadata}")

        // 校验通过
        messageDispatcher.dispatch(state.metadata, tempFile)
        receiveContexts.remove(userId)
    }

    /**
     * 大文件分片：通过 WriteSession 直接 seek 写入预分配文件
     *
     * 整个传输过程复用同一个 FileChannel 句柄，不反复开关。
     */
    private suspend fun handleLargeFileChunk(
        userId: String,
        ctx: ReceiveContext,
        chunkData: ByteArray
    ) = withContext(Dispatchers.IO) {
        val state = ctx.state
        val session = state.writeSession ?: return@withContext

        // 写入预分配的文件
        session.writeAtOffset(state.receivedBytes, chunkData)
        state.receivedBytes += chunkData.size

        // 进度上报（节流）
        if (state.receivedBytes - state.lastReportedAt >= progressInterval) {
            state.lastReportedAt = state.receivedBytes
            messageDispatcher.updateReceiveProgress(
                state.metadata.messageId,
                state.receivedBytes
            )
        }

        if (state.receivedBytes >= state.metadata.fileSize) {
            // 全部分片到齐 → 合并 → 校验 → 分发
            onLargeFileComplete(userId, state)
        }
    }

    /**
     * 大文件接收完成
     *
     * 关闭 WriteSession → SHA256 校验 → 分发 → 发送达回执
     */
    private suspend fun onLargeFileComplete(userId: String, state: MediaReceiveState) {
        val metadata = state.metadata
        val messageId = metadata.messageId

        try {
            // 刷盘 + 关闭 FileChannel
            state.writeSession?.flush()
            state.closeSession()

            // 最终进度拉满
            messageDispatcher.updateReceiveProgress(messageId, metadata.fileSize)

            val dataFile = chunkStorageManager.getCompletedFile(messageId)

            // 哈希值校验
            val actualChecksum = dataFile.toSHA256Hex()
            if (actualChecksum != metadata.checksum) {
                Log.e(
                    TAG,
                    "大文件校验失败 [$messageId]: 期望=${metadata.checksum}, 实际=$actualChecksum"
                )
                chunkStorageManager.cleanup(messageId)
                receiveContexts.remove(userId)
                return
            }

            // 分发（更新 Receiving → Delivered，持久化文件）
            messageDispatcher.dispatch(metadata, dataFile)

            // 清理分片目录
            chunkStorageManager.cleanup(messageId)
            receiveContexts.remove(userId)

            // 发送送达回执
            scope.launch {
                messageSender.sendReceipt(
                    messageId = messageId,
                    receiverId = metadata.senderId,
                    type = ReceiptType.Delivered
                )
            }

            Log.d(TAG, "大文件接收完成: $messageId")
        } catch (e: Exception) {
            Log.e(TAG, "大文件处理失败: $messageId", e)
            state.closeSession()
            receiveContexts.remove(userId)
        }
    }

    /**
     * 处理对方回复的文件元数据 ACK
     */
    private fun handleFileMetaAck(body: ByteArray) {
        val ack = json.decodeFromString<FileMetaAck>(String(body, Charsets.UTF_8))
        Log.d(TAG, "收到 FILE_META_ACK: messageId=${ack.messageId}, status=${ack.status}")
        fileAckRegistry.complete(ack.messageId, ack)
    }

    /**
     * 处理对方发来的取消传输通知
     */
    private suspend fun handleFileCancel(body: ByteArray) {
        val messageId = parseMessageId(body) ?: return
        val message = messageDao.getById(messageId) ?: return
        if (!message.sendStatus.isProgressing) return

        if (message.isFromMe) {
            // 标记取消，将自动停止发送
            transferManager.setCancelled(messageId)
        } else {
            // 清理接收状态
            receiveContexts.entries
                .find { it.value.state.metadata.messageId == messageId }
                ?.let { entry ->
                    entry.value.cleanup()
                    receiveContexts.remove(entry.key)
                }
            // 清理文件分片
            chunkStorageManager.cleanup(messageId)
        }

        // 更新消息状态为失败
        messageDao.update(messageId) {
            it.copy(
                sendStatus = SendStatus.Failed,
                failReason = SendError.Cancelled
            )
        }

        Log.d(TAG, "传输已取消: $messageId")
    }

    /**
     * 处理对方发来的暂停传输通知
     */
    private suspend fun handleFilePause(body: ByteArray) {
        val messageId = parseMessageId(body) ?: return
        val message = messageDao.getById(messageId) ?: return
        if (!message.sendStatus.isProgressing) return

        if (message.isFromMe) {
            // 标记暂停，发送文件的循环将会挂起，直到继续或取消
            transferManager.setPaused(messageId)
        }

        // 更新消息状态
        messageDao.update(messageId) {
            it.copy(sendStatus = SendStatus.Paused)
        }

        Log.d(TAG, "传输已暂停: $messageId")
    }

    /**
     * 处理对方发来的恢复传输通知
     */
    private suspend fun handleFileResume(body: ByteArray) {
        val messageId = parseMessageId(body) ?: return
        val message = messageDao.getById(messageId) ?: return
        if (message.sendStatus != SendStatus.Paused) return

        if (message.isFromMe) {
            if (transferManager.hasActiveTransfer(messageId)) {
                // 协程还在，正常唤醒
                transferManager.setResumed(messageId)
            } else {
                transferManager.remove(messageId)
                // 重启发送协程
                scope.launch {
                    val file = File(message.localPath ?: return@launch)
                    messageSender.sendMediaMessage(message, file)
                }
            }
        }

        // 更新消息状态
        val newStatus = if (message.isFromMe) SendStatus.Sending else SendStatus.Receiving
        messageDao.update(messageId) { it.copy(sendStatus = newStatus) }

        Log.d(TAG, "传输已恢复: $messageId")
    }

    /**
     * 从传输控制包的 body 中解析 messageId
     */
    private fun parseMessageId(body: ByteArray): String? {
        return try {
            val map = json.decodeFromString<Map<String, String>>(String(body, Charsets.UTF_8))
            map["messageId"]
        } catch (e: Exception) {
            Log.e(TAG, "解析 messageId 失败", e)
            null
        }
    }

    /**
     * 发送文件元数据 ACK 给对方
     */
    private suspend fun sendFileMetaAck(
        receiverId: String,
        messageId: String,
        status: FileAckStatus,
        receivedBytes: Long = 0
    ) {
        val ack = FileMetaAck(
            messageId = messageId,
            senderId = myUserId,
            receiverId = receiverId,
            status = status,
            receivedBytes = receivedBytes
        )

        val packet = Packet(
            type = PacketType.FILE_META_ACK,
            body = json.encodeToString(ack).toByteArray(Charsets.UTF_8)
        )

        transport.send(receiverId, packet)
            .onFailure {
                Log.e(TAG, "发送 FILE_META_ACK 失败：$receiverId", it)
            }
    }

    /**
     * 清理未完成的媒体接收状态，关闭流并删除临时文件
     */
    private fun cleanupReceiveContext(userId: String) {
        receiveContexts.remove(userId)?.let { ctx ->
            if (!ctx.isLargeFile) {
                // 小文件：清理临时文件
                ctx.cleanup()
            } else {
                // 大文件：只关 FileChannel，保留数据文件用于断点续传
                ctx.state.closeSession()
            }
            Log.w(TAG, "已清理接收上下文: messageId=${ctx.state.metadata.messageId}")
        }
    }

    private val progressInterval: Long
        get() = when (transport.mode.value) {
            ConnectionMode.Bluetooth -> TransferConfig.PROGRESS_REPORT_INTERVAL_BT
            else -> TransferConfig.PROGRESS_REPORT_INTERVAL
        }
}

/**
 * 媒体接收状态
 */
private data class ReceiveContext(
    val state: MediaReceiveState,
    val isLargeFile: Boolean,
    /** 小文件用：临时文件 */
    val tempFile: File? = null,
    /** 小文件用：写入流 */
    val outputStream: BufferedOutputStream? = null
) {
    fun cleanup() {
        // 小文件：关闭流 + 删除临时文件
        runCatching { outputStream?.close() }
        runCatching { tempFile?.delete() }
        // 大文件：关闭 FileChannel
        state.closeSession()
    }
}