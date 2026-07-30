package top.chengdongqing.wechat.core.network.messaging

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.common.di.IoScope
import top.chengdongqing.wechat.core.data.model.ChatProtocol
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.data.model.ReceiptType
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.data.storage.AssetOwner
import top.chengdongqing.wechat.core.data.storage.AssetOwnerType
import top.chengdongqing.wechat.core.data.storage.AssetReferenceManager
import top.chengdongqing.wechat.core.database.dao.MediaFileDao
import top.chengdongqing.wechat.core.database.dao.MessageDao
import top.chengdongqing.wechat.core.model.SendError
import top.chengdongqing.wechat.core.model.SendStatus
import top.chengdongqing.wechat.core.network.config.TransferConfig
import top.chengdongqing.wechat.core.network.connection.ChatTransportManager
import top.chengdongqing.wechat.core.network.connection.ConnectionEvent
import top.chengdongqing.wechat.core.network.connection.PeerConnection
import top.chengdongqing.wechat.core.network.model.FileAckStatus
import top.chengdongqing.wechat.core.network.model.FileMetaAck
import top.chengdongqing.wechat.core.network.model.Packet
import top.chengdongqing.wechat.core.network.model.PacketType
import top.chengdongqing.wechat.core.network.transfer.TransferManager
import top.chengdongqing.wechat.core.util.toSHA256Hex
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息接收器
 */
@Singleton
class MessageReceiver @Inject constructor(
    private val transport: ChatTransportManager,
    private val messageDispatcher: MessageDispatcher,
    private val meshGroupRouter: MeshGroupRouter,
    private val permissionChecker: MessagePermissionChecker,
    private val messageSender: MessageSender,
    private val profileRepository: ProfileRepository,
    private val assetReferenceManager: AssetReferenceManager,
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
    private val started = AtomicBoolean(false)

    private val myUserId: String
        get() = profileRepository.requireUserId()

    /**
     * 启动监听，订阅新连接并自动开始消费
     */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        messageSender.startRetryScheduler()
        scope.launch {
            transport.connectionEvents.collect { event ->
                when (event) {
                    is ConnectionEvent.Connected -> {
                        startListening(event.conn)
                        scope.launch { messageSender.retryPendingMessages(event.userId) }
                    }

                    is ConnectionEvent.Disconnected -> {
                        Log.d(TAG, "连接断开: ${event.userId} - ${event.reason}")
                    }
                }
            }
        }
    }

    /**
     * 开始监听数据
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
                // 连接断开时清理该用户所有未完成的接收上下文
                cleanupReceiveContextsByUser(userId)
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
                PacketType.FILE_CHUNK -> handleFileChunk(packet.body)

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
        }
    }

    /**
     * 处理JSON包
     */
    private suspend fun handleJsonPacket(userId: String, packet: Packet) {
        val protocol = json.decodeFromString<ChatProtocol>(String(packet.body, Charsets.UTF_8))

        when {
            // 回执消息不判断
            protocol is ChatProtocol.MessageReceipt -> Unit
            // 权限校验
            !permissionChecker.checkAndReply(protocol.senderId, protocol) -> return
        }

        if (protocol is ChatProtocol.GroupTextMessage) {
            meshGroupRouter.relay(protocol, receivedFrom = userId)
        }
        // 将消息分发下去
        messageDispatcher.dispatch(protocol)
    }

    /**
     * 处理文件元数据包
     */
    private suspend fun handleFileMeta(userId: String, body: ByteArray) {
        val metadata = json.decodeFromString<ChatProtocol.MediaMessage>(
            String(body, Charsets.UTF_8)
        )

        // 同一 messageId 重传时，清理旧上下文（正常流程不会触发）
        receiveContexts.remove(metadata.messageId)?.let { stale ->
            Log.w(TAG, "收到重复 FILE_META，清理旧上下文: ${metadata.messageId}")
            if (!stale.isLargeFile) stale.cleanup() else stale.state.closeSession()
        }

        // 权限校验
        if (!permissionChecker.checkAndReply(metadata.senderId, metadata)) {
            return
        }

        // 达到指定阈值时走大文件分片传输，否则走小文件直传
        if (metadata.fileSize < TransferConfig.CHUNK_TRANSFER_THRESHOLD) {
            handleSmallFileMeta(userId, metadata)
        } else {
            handleLargeFileMeta(userId, metadata)
        }
    }

    /**
     * 小文件：不发 ACK，创建临时文件 + BufferedOutputStream 等待 CHUNK 追加
     */
    private suspend fun handleSmallFileMeta(
        userId: String,
        metadata: ChatProtocol.MediaMessage
    ) = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "recv_${metadata.messageId}.tmp")
        val outputStream = BufferedOutputStream(FileOutputStream(tempFile), DISK_WRITE_BUFFER)

        receiveContexts[metadata.messageId] = ReceiveContext(
            userId = userId,
            state = ReceiveState(metadata = metadata),
            isLargeFile = false,
            tempFile = tempFile,
            outputStream = outputStream
        )
    }

    /**
     * 大文件：走协商流程（秒传 / 断点续传 / 全新传输）
     */
    private suspend fun handleLargeFileMeta(userId: String, metadata: ChatProtocol.MediaMessage) {
        // 判断是否可以秒传
        val existingFile = mediaFileDao.getByChecksum(metadata.checksum)
        if (existingFile != null) {
            assetReferenceManager.attach(
                existingFile.localPath,
                metadata.checksum,
                AssetOwner(AssetOwnerType.Message, metadata.messageId)
            )
            messageDispatcher.dispatchExistingMedia(metadata, existingFile.localPath)
            sendFileMetaAck(userId, metadata.messageId, FileAckStatus.AlreadyExists)
            return
        }

        // 查断点：数据库 sentBytes + 磁盘数据文件
        val existingMessage = messageDao.getById(metadata.messageId)
        val receivedBytes = existingMessage?.sentBytes ?: 0L
        val hasPendingFile = chunkStorageManager.hasPendingTransfer(metadata.messageId)

        // 判断是否可以断点续传
        if (receivedBytes > 0 && hasPendingFile && receivedBytes < metadata.fileSize) {
            val writeSession = chunkStorageManager.resumeTransfer(metadata.messageId, receivedBytes)
            if (writeSession != null) {
                receiveContexts[metadata.messageId] = ReceiveContext(
                    userId = userId,
                    state = ReceiveState(
                        metadata = metadata,
                        receivedBytes = receivedBytes,
                        lastReportedAt = receivedBytes,
                        writeSession = writeSession
                    ),
                    isLargeFile = true
                )

                sendFileMetaAck(userId, metadata.messageId, FileAckStatus.ResumeFrom, receivedBytes)
                return
            }

            Log.w(TAG, "恢复写入会话失败，重新开始: ${metadata.messageId}")
            chunkStorageManager.cleanup(metadata.messageId)
        }

        // 走全新传输
        val writeSession = chunkStorageManager.initTransfer(metadata)
        receiveContexts[metadata.messageId] = ReceiveContext(
            userId = userId,
            state = ReceiveState(metadata = metadata, writeSession = writeSession),
            isLargeFile = true
        )

        messageDispatcher.createReceivingMessage(metadata)

        sendFileMetaAck(userId, metadata.messageId, FileAckStatus.ReadyToReceive)
    }

    /**
     * 处理文件分片包
     *
     * 不同文件的分片可以在同一连接上并发到达，各自路由到独立上下文，互不干扰
     */
    private suspend fun handleFileChunk(body: ByteArray) {
        val (messageId, offset, chunkData) = try {
            FileChunkCodec.decode(body)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "FILE_CHUNK body 格式非法，丢弃", e)
            return
        }

        val ctx = receiveContexts[messageId] ?: run {
            Log.w(TAG, "收到 FILE_CHUNK 但无对应 FILE_META (messageId=$messageId)")
            return
        }

        if (ctx.isLargeFile) {
            handleLargeFileChunk(messageId, ctx, offset, chunkData)
        } else {
            handleSmallFileChunk(messageId, ctx, chunkData)
        }
    }

    /**
     * 小文件分片：追加写入同一个临时文件
     */
    private suspend fun handleSmallFileChunk(
        messageId: String,
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

        // 哈希值校验
        val actualChecksum = tempFile.toSHA256Hex()
        if (actualChecksum != state.metadata.checksum) {
            Log.e(
                TAG,
                "小文件校验失败 [$messageId]: 期望=${state.metadata.checksum}, 实际=$actualChecksum"
            )
            tempFile.delete()
            receiveContexts.remove(messageId)
            return@withContext
        }

        // 校验通过，分发下去
        messageDispatcher.dispatch(state.metadata, tempFile)
        receiveContexts.remove(messageId)

        Log.d(TAG, "小文件接收完成: ${state.metadata}")
    }

    /**
     * 大文件分片：通过 WriteSession 直接 seek 写入预分配文件
     *
     * 整个传输过程复用同一个 FileChannel 句柄
     */
    private suspend fun handleLargeFileChunk(
        messageId: String,
        ctx: ReceiveContext,
        offset: Long,
        chunkData: ByteArray
    ) = withContext(Dispatchers.IO) {
        val state = ctx.state
        val session = state.writeSession ?: return@withContext

        // 写入预分配的文件
        session.writeAtOffset(offset, chunkData)
        // 进度更新逻辑为：已接收最大边界
        state.receivedBytes = maxOf(state.receivedBytes, offset + chunkData.size)

        // 进度上报
        if (state.receivedBytes - state.lastReportedAt >= progressInterval) {
            state.lastReportedAt = state.receivedBytes
            messageDispatcher.updateReceiveProgress(messageId, state.receivedBytes)
        }

        if (state.receivedBytes >= state.metadata.fileSize) {
            // 全部分片到齐 → 合并 → 校验 → 分发
            onLargeFileComplete(messageId, state)
        }
    }

    /**
     * 大文件接收完成
     */
    private suspend fun onLargeFileComplete(messageId: String, state: ReceiveState) {
        val metadata = state.metadata

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
                receiveContexts.remove(messageId)
                messageDao.update(messageId) {
                    it.copy(
                        sendStatus = SendStatus.Failed,
                        failReason = SendError.Unknown
                    )
                }
                return
            }

            // 校验通过，分发下去
            messageDispatcher.dispatch(metadata, dataFile)
            chunkStorageManager.cleanup(messageId)
            receiveContexts.remove(messageId)

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
            receiveContexts.remove(messageId)
        }
    }

    /**
     * 处理对方回复的文件元数据 ACK
     */
    private fun handleFileMetaAck(body: ByteArray) {
        val ack = json.decodeFromString<FileMetaAck>(String(body, Charsets.UTF_8))
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
            receiveContexts.remove(messageId)?.cleanup()
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
                // 重启发送协程（一般在app重启后）
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

        transport.send(
            receiverId,
            Packet(
                type = PacketType.FILE_META_ACK,
                body = json.encodeToString(ack).toByteArray(Charsets.UTF_8)
            )
        ).onFailure {
            Log.e(TAG, "发送 FILE_META_ACK 失败：$receiverId", it)
        }
    }

    /**
     * 清理指定用户的所有未完成接收上下文
     */
    private fun cleanupReceiveContextsByUser(userId: String) {
        val iterator = receiveContexts.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.userId != userId) continue

            val ctx = entry.value
            iterator.remove()

            if (!ctx.isLargeFile) {
                // 小文件：清理临时文件
                ctx.cleanup()
            } else {
                // 大文件：保留数据文件，用于断点续传
                ctx.state.closeSession()
            }
        }
    }

    private fun parseMessageId(body: ByteArray): String? {
        return try {
            val map = json.decodeFromString<Map<String, String>>(String(body, Charsets.UTF_8))
            map["messageId"]
        } catch (e: Exception) {
            Log.e(TAG, "解析 messageId 失败", e)
            null
        }
    }

    private val progressInterval: Long
        get() = when (transport.mode.value) {
            ConnectionMode.Bluetooth -> TransferConfig.PROGRESS_REPORT_INTERVAL_BT
            else -> TransferConfig.PROGRESS_REPORT_INTERVAL
        }
}
