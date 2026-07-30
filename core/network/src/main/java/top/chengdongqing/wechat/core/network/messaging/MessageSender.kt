package top.chengdongqing.wechat.core.network.messaging

import android.util.Log
import androidx.room3.withWriteTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.common.di.IoScope
import top.chengdongqing.wechat.core.common.file.extractExtension
import top.chengdongqing.wechat.core.data.model.ChatProtocol
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.data.model.MessageQuote
import top.chengdongqing.wechat.core.data.model.ReceiptType
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.data.storage.AssetOwner
import top.chengdongqing.wechat.core.data.storage.AssetOwnerType
import top.chengdongqing.wechat.core.data.storage.AssetReferenceManager
import top.chengdongqing.wechat.core.database.WeDatabase
import top.chengdongqing.wechat.core.database.dao.ChatSessionDao
import top.chengdongqing.wechat.core.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.core.database.dao.GroupDao
import top.chengdongqing.wechat.core.database.dao.MediaFileDao
import top.chengdongqing.wechat.core.database.dao.MessageDao
import top.chengdongqing.wechat.core.database.entity.MessageEntity
import top.chengdongqing.wechat.core.model.ProfileBeacon
import top.chengdongqing.wechat.core.model.SendError
import top.chengdongqing.wechat.core.model.SendStatus
import top.chengdongqing.wechat.core.network.config.TransferConfig
import top.chengdongqing.wechat.core.network.connection.ChatTransportManager
import top.chengdongqing.wechat.core.network.connection.ConnectionException
import top.chengdongqing.wechat.core.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.core.network.crypto.PacketSigner
import top.chengdongqing.wechat.core.network.http.AvatarServer
import top.chengdongqing.wechat.core.network.model.FileAckStatus
import top.chengdongqing.wechat.core.network.model.Packet
import top.chengdongqing.wechat.core.network.model.PacketType
import top.chengdongqing.wechat.core.network.security.KeyStoreManager
import top.chengdongqing.wechat.core.network.transfer.TransferManager
import top.chengdongqing.wechat.core.network.transfer.WiFiLockManager
import top.chengdongqing.wechat.core.util.toSHA256Hex
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * 消息发送器
 */
@Singleton
class MessageSender @Inject constructor(
    private val database: WeDatabase,
    private val transport: ChatTransportManager,
    private val connectionInfoDao: ConnectionInfoDao,
    private val messageDao: MessageDao,
    private val groupDao: GroupDao,
    private val chatSessionDao: ChatSessionDao,
    private val wifiLockManager: WiFiLockManager,
    private val transferManager: TransferManager,
    private val profileRepository: ProfileRepository,
    private val packetSigner: PacketSigner,
    private val keyStoreManager: KeyStoreManager,
    private val avatarServer: AvatarServer,
    private val mediaFileDao: MediaFileDao,
    private val assetReferenceManager: AssetReferenceManager,
    private val fileAckRegistry: FileAckRegistry,
    private val json: Json,
    @param:IoScope private val scope: CoroutineScope
) {
    private companion object {
        const val TAG = "MessageSender"
        const val ACK_TIMEOUT_MS = 10_000L // 发大文件时等待回执的超时时间
        const val MAX_TOTAL_ATTEMPTS = 6
        const val RETRY_BASE_DELAY_MS = 750L
        const val ACK_DELIVERY_TIMEOUT_MS = 12_000L
        const val RETRY_POLL_INTERVAL_MS = 2_000L
    }

    private val myUserId: String
        get() = profileRepository.requireUserId()

    private val retryLocks = ConcurrentHashMap<String, Mutex>()
    private val retrySchedulerStarted = AtomicBoolean(false)

    fun startRetryScheduler() {
        if (!retrySchedulerStarted.compareAndSet(false, true)) return
        scope.launch {
            while (isActive) {
                delay(RETRY_POLL_INTERVAL_MS)
                val now = System.currentTimeMillis()
                messageDao.failExhaustedAckWaits(now, MAX_TOTAL_ATTEMPTS)
                val due = messageDao.getDueOutgoing(
                    now = now,
                    maxAttempts = MAX_TOTAL_ATTEMPTS
                )
                due.forEach { message ->
                    if (!transport.isConnected(message.receiverId)) return@forEach
                    retryLocks.getOrPut(message.receiverId) { Mutex() }.withLock {
                        if (transport.isConnected(message.receiverId)) {
                            messageDao.update(message.id) {
                                it.copy(sendStatus = SendStatus.Sending, nextRetryAt = null)
                            }
                            resend(message)
                        }
                    }
                }
            }
        }
    }

    /**
     * 连接恢复后重放未获得送达回执的消息。
     *
     * 接收端以 messageId 幂等，重放 Sent 状态可以覆盖“已写入 Socket，
     * 但送达回执在断线时丢失”的窗口。
     */
    suspend fun retryPendingMessages(peerId: String) {
        retryLocks.getOrPut(peerId) { Mutex() }.withLock {
            val pending = messageDao.getPendingOutgoing(peerId).filter { message ->
                message.attemptCount < MAX_TOTAL_ATTEMPTS &&
                        (message.sendStatus != SendStatus.Failed ||
                                message.failReason?.canRetry != false &&
                                message.failReason != SendError.Cancelled)
            }
            if (pending.isEmpty()) return

            Log.i(TAG, "重连补发 ${pending.size} 条消息: $peerId")
            pending.forEach { message ->
                if (!transport.isConnected(peerId)) return
                messageDao.update(message.id) {
                    it.copy(sendStatus = SendStatus.Sending, failReason = null, nextRetryAt = null)
                }
                resend(message).onFailure { Log.w(TAG, "补发失败: ${message.id}", it) }
            }
        }
    }

    private suspend fun resend(message: MessageEntity): Result<Unit> = runCatching {
        when {
            groupDao.getById(message.sessionId) != null && message.localPath == null ->
                sendGroupTextMessage(message).getOrThrow()

            message.localPath != null -> {
                val file = File(message.localPath!!)
                require(file.exists()) { "待发送文件不存在" }
                sendMediaMessage(message, file).getOrThrow()
            }

            else -> sendTextMessage(message).getOrThrow()
        }
    }.onFailure { error ->
        Log.w(TAG, "补发尝试失败: ${message.id}", error)
    }

    /**
     * 发送文本消息
     */
    suspend fun sendTextMessage(message: MessageEntity): Result<Unit> {
        markAttemptStarted(message.id)
        val protocol = ChatProtocol.TextMessage(
            messageId = message.id,
            senderId = message.senderId,
            receiverId = message.receiverId,
            signature = "",
            messageType = message.contentType,
            content = message.content,
            quote = message.toQuote(),
            timestamp = message.timestamp
        )
        val signature = packetSigner.sign(protocol, keyStoreManager.getPrivateKey())

        val packet = Packet(
            PacketType.TEXT,
            serializeChatProtocol(protocol.copy(signature = signature))
        )

        return transport.send(message.receiverId, packet)
            .onSuccess {
                updateStatus(
                    messageId = message.id,
                    sessionId = message.receiverId
                )
            }
            .onFailure { e ->
                handleSendError(message.id, message.receiverId, e)
                throw e
            }
    }

    suspend fun sendGroupTextMessage(message: MessageEntity): Result<Unit> {
        markAttemptStarted(message.id)
        val group = groupDao.getById(message.sessionId)
            ?: return Result.failure(IllegalStateException("群聊不存在"))
        val unsigned = ChatProtocol.GroupTextMessage(
            messageId = message.id,
            senderId = message.senderId,
            signature = "",
            timestamp = message.timestamp,
            groupId = group.id,
            memberVersion = group.memberVersion,
            messageType = message.contentType,
            content = message.content,
            quote = message.toQuote()
        )
        val protocol = unsigned.copy(
            signature = packetSigner.sign(unsigned, keyStoreManager.getPrivateKey())
        )
        val packet = Packet(PacketType.TEXT, serializeChatProtocol(protocol))
        val targets = groupDao.getMembers(group.id)
            .map { it.userId }
            .filter { it != myUserId }

        val results = targets.map { target -> transport.send(target, packet) }
        val delivered = results.count { it.isSuccess }
        return if (delivered > 0 || targets.isEmpty()) {
            updateStatus(message.id, message.sessionId)
            Result.success(Unit)
        } else {
            val error = results.firstNotNullOfOrNull { it.exceptionOrNull() }
                ?: IllegalStateException("没有可达的群成员")
            handleSendError(message.id, message.sessionId, error)
            Result.failure(error)
        }
    }

    /**
     * 发送媒体消息
     */
    suspend fun sendMediaMessage(message: MessageEntity, file: File): Result<Unit> = runCatching {
        markAttemptStarted(message.id)
        val checksum = file.toSHA256Hex()
        val targetFile = resolveTargetFile(message, file, checksum)
        val metadata = buildSignedMetadata(message, targetFile, checksum)

        if (targetFile.length() < TransferConfig.CHUNK_TRANSFER_THRESHOLD) {
            sendSmallFile(message, metadata, targetFile)
        } else {
            sendLargeFile(message, metadata, targetFile)
        }

        updateStatus(
            messageId = message.id,
            sessionId = message.receiverId
        )
    }.onFailure { e ->
        handleSendError(message.id, message.receiverId, e)
        throw e
    }

    /**
     * 构建已签名的媒体消息元数据
     */
    private fun buildSignedMetadata(
        message: MessageEntity,
        targetFile: File,
        checksum: String
    ): ChatProtocol.MediaMessage {
        val unsigned = ChatProtocol.MediaMessage(
            messageId = message.id,
            senderId = message.senderId,
            receiverId = message.receiverId,
            signature = "",
            messageType = message.contentType,
            content = message.content,
            extension = message.localPath?.extractExtension(),
            fileSize = targetFile.length(),
            checksum = checksum,
            mediaDuration = message.mediaDuration,
            quote = message.toQuote(),
            timestamp = message.timestamp
        )
        val signature = packetSigner.sign(unsigned, keyStoreManager.getPrivateKey())
        return unsigned.copy(signature = signature)
    }

    private fun MessageEntity.toQuote(): MessageQuote? =
        quoteMessageId?.let {
            MessageQuote(
                messageId = it,
                senderId = quoteSenderId.orEmpty(),
                messageType = quoteMessageType
                    ?: top.chengdongqing.wechat.core.model.MessageType.Text,
                preview = quotePreview.orEmpty()
            )
        }

    /**
     * 小文件直传
     */
    private suspend fun sendSmallFile(
        message: MessageEntity,
        metadata: ChatProtocol.MediaMessage,
        file: File
    ) {
        wifiLockManager.withTransferLock {
            transport.sendAtomicTransfer(message.receiverId) { writer ->
                // 发送文件元数据
                writer.write(
                    Packet(
                        type = PacketType.FILE_META,
                        body = serializeChatProtocol(metadata)
                    )
                )
                // 发送文件内容
                sendFileChunks(writer, file, message.id)
            }.getOrThrow()
        }
    }

    /**
     * 大文件协商式传输
     */
    private suspend fun sendLargeFile(
        message: MessageEntity,
        metadata: ChatProtocol.MediaMessage,
        file: File
    ) {
        // 发送文件元数据
        val metaPacket = Packet(
            type = PacketType.FILE_META,
            body = serializeChatProtocol(metadata)
        )
        transport.send(message.receiverId, metaPacket).getOrThrow()

        // 等待对方的回执
        val ack = fileAckRegistry.awaitAck(message.id, ACK_TIMEOUT_MS)

        // 根据 ACK 状态决定后续操作
        when (ack.status) {
            FileAckStatus.AlreadyExists -> {
                Log.d(TAG, "对方已有文件，跳过传输: ${message.id}")
            }

            FileAckStatus.ResumeFrom -> {
                Log.d(TAG, "断点续传: ${message.id}, offset=${ack.receivedBytes}")
                sendFileData(message.receiverId, file, message.id, offset = ack.receivedBytes)
            }

            FileAckStatus.ReadyToReceive -> {
                sendFileData(message.receiverId, file, message.id, offset = 0)
            }
        }
    }

    /**
     * 发送文件数据（大文件分片传输）
     */
    private suspend fun sendFileData(
        receiverId: String,
        file: File,
        messageId: String,
        offset: Long
    ) {
        wifiLockManager.withTransferLock {
            transport.sendAtomicTransfer(receiverId) { writer ->
                streamFileChunks(file, file.length(), messageId, offset) { encodedChunk ->
                    writer.writeNoFlush(Packet(PacketType.FILE_CHUNK, encodedChunk))
                }
            }.getOrThrow()
        }
    }

    // -------------------------------------------------------------------------
    // 传输控制
    // -------------------------------------------------------------------------

    suspend fun sendFileCancel(receiverId: String, messageId: String): Result<Unit> =
        sendTransferControl(receiverId, messageId, PacketType.FILE_CANCEL, "取消")

    suspend fun sendFilePause(receiverId: String, messageId: String): Result<Unit> =
        sendTransferControl(receiverId, messageId, PacketType.FILE_PAUSE, "暂停")

    suspend fun sendFileResume(receiverId: String, messageId: String): Result<Unit> =
        sendTransferControl(receiverId, messageId, PacketType.FILE_RESUME, "恢复")

    private suspend fun sendTransferControl(
        receiverId: String,
        messageId: String,
        type: Byte,
        label: String
    ): Result<Unit> {
        val body = json.encodeToString(
            mapOf("messageId" to messageId)
        ).toByteArray(Charsets.UTF_8)

        return transport.send(receiverId, Packet(type = type, body = body))
            .onFailure { Log.w(TAG, "发送${label}通知失败: $receiverId") }
    }

    // -------------------------------------------------------------------------
    // 回执 / 个人资料
    // -------------------------------------------------------------------------

    suspend fun sendReceipt(messageId: String, receiverId: String, type: ReceiptType) {
        val protocol = ChatProtocol.MessageReceipt(
            messageId = messageId,
            senderId = myUserId,
            receiverId = receiverId,
            receiptType = type,
            signature = "",
            timestamp = System.currentTimeMillis()
        )
        val signature = packetSigner.sign(protocol, keyStoreManager.getPrivateKey())

        transport.send(
            receiverId,
            Packet(PacketType.RECEIPT, serializeChatProtocol(protocol.copy(signature = signature)))
        ).onFailure {
            Log.w(TAG, "回执发送失败: $receiverId")
        }
    }

    suspend fun sendProfile(userId: String) {
        val profile = profileRepository.requireProfile()

        val beacon = ProfileBeacon(
            userId = profile.id,
            nickname = profile.nickname,
            gender = profile.gender,
            signature = profile.signature,
            avatarUrl = avatarServer.avatarUrl,
            publicKey = profile.publicKey
        )
        val protocol = ChatProtocol.ProfileResponse(
            senderId = profile.id,
            profile = beacon,
            signature = ""
        )
        val signature = packetSigner.sign(protocol, keyStoreManager.getPrivateKey())

        transport.send(
            userId,
            Packet(
                type = PacketType.PROFILE_RESPONSE,
                body = json.encodeToString<ChatProtocol>(
                    protocol.copy(signature = signature)
                ).toByteArray(Charsets.UTF_8)
            )
        )
    }

    // -------------------------------------------------------------------------
    // 内部工具
    // -------------------------------------------------------------------------

    private suspend fun sendFileChunks(
        writer: EncryptingPacketWriter,
        file: File,
        messageId: String,
        offset: Long = 0
    ) {
        streamFileChunks(file, file.length(), messageId, offset) { encodedChunk ->
            writer.writeNoFlush(Packet(PacketType.FILE_CHUNK, encodedChunk))
        }
    }

    /**
     * 流式读文件，将每个分片通过 [FileChunkCodec.encode] 打包后回调
     */
    private suspend inline fun streamFileChunks(
        file: File,
        fileSize: Long,
        messageId: String,
        offset: Long = 0,
        crossinline onChunk: suspend (ByteArray) -> Unit
    ) = withContext(Dispatchers.IO) {
        FileChannel.open(file.toPath(), StandardOpenOption.READ).use { channel ->
            if (offset > 0) channel.position(offset)

            val buffer = ByteBuffer.allocate(fileChunkSize)
            var totalSent = offset
            var lastReportedAt = offset

            while (true) {
                // 检测暂停
                transferManager.awaitIfPaused(messageId)

                // 检测取消
                if (transferManager.isCancelled(messageId)) {
                    transferManager.remove(messageId)
                    throw CancellationException("已取消发送")
                }

                buffer.clear()
                val bytesRead = channel.read(buffer)
                if (bytesRead <= 0) break

                buffer.flip()
                val currentOffset = totalSent // 记录当前读取的位置
                val rawChunk = ByteArray(buffer.remaining()).also { buffer.get(it) }
                onChunk(FileChunkCodec.encode(messageId, currentOffset, rawChunk))

                totalSent += bytesRead
                if (fileSize > 0 && totalSent - lastReportedAt >= progressInterval) {
                    lastReportedAt = totalSent
                    updateProgress(messageId, totalSent)
                }
            }

            if (totalSent > lastReportedAt) {
                updateProgress(messageId, totalSent)
            }
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
    private suspend fun updateStatus(
        messageId: String,
        sessionId: String,
        status: SendStatus = SendStatus.Sent
    ) {
        database.withWriteTransaction {
            messageDao.update(messageId) { message ->
                message.copy(
                    sendStatus = status,
                    ackDeadlineAt = if (status == SendStatus.Sent) {
                        System.currentTimeMillis() + ACK_DELIVERY_TIMEOUT_MS
                    } else null,
                    nextRetryAt = null,
                    failReason = null
                )
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
        val failReason = when (error) {
            is ConnectionException -> error.failReason
            is CancellationException -> SendError.Cancelled
            else -> SendError.Unknown
        }

        database.withWriteTransaction {
            // 更新消息状态
            messageDao.update(messageId) { message ->
                val retryDelay = RETRY_BASE_DELAY_MS *
                        (1L shl message.attemptCount.coerceIn(0, 5))
                message.copy(
                    sendStatus = SendStatus.Failed,
                    failReason = failReason,
                    ackDeadlineAt = null,
                    nextRetryAt = if (
                        failReason.canRetry &&
                        failReason != SendError.Cancelled &&
                        message.attemptCount < MAX_TOTAL_ATTEMPTS
                    ) System.currentTimeMillis() + retryDelay else null
                )
            }
            // 更新会话状态
            chatSessionDao.update(receiverId) { session ->
                session.copy(isSending = false)
            }
        }

        // 标记为离线
        if (error !is CancellationException) {
            connectionInfoDao.markOffline(receiverId)
        }
    }

    private suspend fun markAttemptStarted(messageId: String) {
        val now = System.currentTimeMillis()
        messageDao.update(messageId) { message ->
            message.copy(
                attemptCount = message.attemptCount + 1,
                lastAttemptAt = now,
                nextRetryAt = null,
                ackDeadlineAt = null,
                lastTransportType = transport.mode.value.name
            )
        }
    }

    /**
     * 文件去重：checksum 已存在则复用原文件，否则注册新文件
     * 返回实际用于传输的目标文件
     */
    private suspend fun resolveTargetFile(
        message: MessageEntity,
        file: File,
        checksum: String
    ): File {
        val existingFile = mediaFileDao.getByChecksum(checksum)

        return if (existingFile != null) {
            assetReferenceManager.attach(
                existingFile.localPath,
                checksum,
                AssetOwner(AssetOwnerType.Message, message.id)
            )
            if (existingFile.localPath == file.absolutePath) return file
            messageDao.update(message.id) { it.copy(localPath = existingFile.localPath) }
            // 不能在发送流程中删除传入文件：它可能属于音乐曲库、转发来源，
            // 或被尚未登记到 media_files 的业务记录持有。这里只切换消息引用；
            // 物理文件统一由 AssetReferenceManager 负责清理。
            File(existingFile.localPath)
        } else {
            assetReferenceManager.attach(
                file.absolutePath,
                checksum,
                AssetOwner(AssetOwnerType.Message, message.id)
            )
            file
        }
    }

    private fun serializeChatProtocol(protocol: ChatProtocol): ByteArray =
        json.encodeToString<ChatProtocol>(protocol).toByteArray(Charsets.UTF_8)

    private val progressInterval: Long
        get() = when (transport.mode.value) {
            ConnectionMode.Bluetooth -> TransferConfig.PROGRESS_REPORT_INTERVAL_BT
            else -> TransferConfig.PROGRESS_REPORT_INTERVAL
        }

    private val fileChunkSize: Int
        get() = when (transport.mode.value) {
            ConnectionMode.Bluetooth -> TransferConfig.FILE_CHUNK_SIZE_BT
            else -> TransferConfig.FILE_CHUNK_SIZE
        }
}
