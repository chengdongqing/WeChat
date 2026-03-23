package top.chengdongqing.wechat.data.network.messaging

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.model.SendError
import top.chengdongqing.wechat.core.model.SendStatus
import top.chengdongqing.wechat.core.util.extractExtension
import top.chengdongqing.wechat.core.util.toSHA256Hex
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.dao.MediaFileDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.connection.ChatTransportManager
import top.chengdongqing.wechat.data.network.connection.ConnectionException
import top.chengdongqing.wechat.data.network.connection.ConnectionMode
import top.chengdongqing.wechat.data.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.data.network.crypto.PacketSigner
import top.chengdongqing.wechat.data.network.http.AvatarServer
import top.chengdongqing.wechat.data.network.model.ChatProtocol
import top.chengdongqing.wechat.data.network.model.FileAckStatus
import top.chengdongqing.wechat.data.network.model.Packet
import top.chengdongqing.wechat.data.network.model.PacketType
import top.chengdongqing.wechat.data.network.model.ReceiptType
import top.chengdongqing.wechat.data.network.transfer.TransferManager
import top.chengdongqing.wechat.data.network.transfer.WiFiLockManager
import top.chengdongqing.wechat.data.security.KeyStoreManager
import top.chengdongqing.wechat.data.session.FileReferenceManager
import top.chengdongqing.wechat.features.profile.data.model.ProfileBeacon
import top.chengdongqing.wechat.features.profile.domain.repository.ProfileRepository
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
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
    private val chatSessionDao: ChatSessionDao,
    private val wifiLockManager: WiFiLockManager,
    private val transferManager: TransferManager,
    private val profileRepository: ProfileRepository,
    private val packetSigner: PacketSigner,
    private val keyStoreManager: KeyStoreManager,
    private val avatarServer: AvatarServer,
    private val mediaFileDao: MediaFileDao,
    private val fileReferenceManager: FileReferenceManager,
    private val privateFileManager: PrivateFileManager,
    private val fileAckRegistry: FileAckRegistry,
    private val json: Json
) {
    private companion object {
        const val TAG = "MessageSender"
        const val ACK_TIMEOUT_MS = 10_000L // 发大文件时等待回执的超时时间
    }

    private val myUserId: String
        get() = profileRepository.requireUserId()

    /**
     * 发送文本消息
     */
    suspend fun sendTextMessage(message: MessageEntity): Result<Unit> {
        val protocol = ChatProtocol.TextMessage(
            messageId = message.id,
            senderId = message.senderId,
            receiverId = message.receiverId,
            signature = "",
            messageType = message.contentType,
            content = message.content,
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

    /**
     * 发送媒体消息
     */
    suspend fun sendMediaMessage(message: MessageEntity, file: File): Result<Unit> = runCatching {
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
            timestamp = message.timestamp
        )
        val signature = packetSigner.sign(unsigned, keyStoreManager.getPrivateKey())
        return unsigned.copy(signature = signature)
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
            }.onFailure {
                // 失败后回滚引用计数
                fileReferenceManager.release(file.absolutePath)
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
            }.onFailure {
                // 失败后回滚引用计数
                fileReferenceManager.release(file.absolutePath)
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
                val rawChunk = ByteArray(buffer.remaining()).also { buffer.get(it) }
                onChunk(FileChunkCodec.encode(messageId, rawChunk))

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
        database.withTransaction {
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
        val failReason = when (error) {
            is ConnectionException -> error.failReason
            is CancellationException -> SendError.Cancelled
            else -> SendError.Unknown
        }

        database.withTransaction {
            // 更新消息状态
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
        if (error !is CancellationException) {
            connectionInfoDao.markOffline(receiverId)
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
            if (existingFile.localPath == file.absolutePath) {
                return file
            }

            fileReferenceManager.retain(existingFile.localPath, checksum)
            messageDao.update(message.id) { it.copy(localPath = existingFile.localPath) }
            privateFileManager.deleteFile(file.absolutePath)
            File(existingFile.localPath)
        } else {
            fileReferenceManager.retain(file.absolutePath, checksum)
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