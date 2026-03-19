package top.chengdongqing.wechat.data.network.messaging

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.util.extractExtension
import top.chengdongqing.wechat.core.util.toSHA256Hex
import top.chengdongqing.wechat.data.database.WeDatabase
import top.chengdongqing.wechat.data.database.dao.ChatSessionDao
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.dao.MediaFileDao
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.data.database.entity.MessageEntity
import top.chengdongqing.wechat.data.model.SendError
import top.chengdongqing.wechat.data.model.SendStatus
import top.chengdongqing.wechat.data.network.avatar.AvatarServer
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.connection.ChatTransportManager
import top.chengdongqing.wechat.data.network.connection.ConnectionException
import top.chengdongqing.wechat.data.network.connection.ConnectionMode
import top.chengdongqing.wechat.data.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.data.network.model.ChatProtocol
import top.chengdongqing.wechat.data.network.model.Packet
import top.chengdongqing.wechat.data.network.model.PacketType
import top.chengdongqing.wechat.data.network.model.ReceiptType
import top.chengdongqing.wechat.data.network.signature.PacketSigner
import top.chengdongqing.wechat.data.network.transfer.TransferManager
import top.chengdongqing.wechat.data.network.transfer.WifiLockManager
import top.chengdongqing.wechat.data.security.LocalIdentity
import top.chengdongqing.wechat.data.session.FileReferenceManager
import top.chengdongqing.wechat.features.me.data.model.ProfileBeacon
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
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
    private val database: WeDatabase,
    private val transport: ChatTransportManager,
    private val connectionInfoDao: ConnectionInfoDao,
    private val messageDao: MessageDao,
    private val chatSessionDao: ChatSessionDao,
    private val wifiLockManager: WifiLockManager,
    private val transferManager: TransferManager,
    private val profileRepository: ProfileRepository,
    private val packetSigner: PacketSigner,
    private val localIdentity: LocalIdentity,
    private val avatarServer: AvatarServer,
    private val mediaFileDao: MediaFileDao,
    private val fileReferenceManager: FileReferenceManager,
    private val privateFileManager: PrivateFileManager,
    private val json: Json
) {
    private companion object {
        const val TAG = "MessageSender"
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
        val signature = packetSigner.sign(protocol, localIdentity.getPrivateKey())

        val packet = Packet(
            PacketType.TEXT,
            serializePolymorphic(protocol.copy(signature = signature))
        )

        return transport.send(message.receiverId, packet)
            .onSuccess {
                updateStatus(
                    messageId = message.id,
                    sessionId = message.receiverId,
                    status = SendStatus.Sent
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
        val targetFile = resolveTargetFile(message, file)
        val metadata = buildSignedMetadata(message, targetFile)

        wifiLockManager.withTransferLock {
            transport.sendAtomicTransfer(message.receiverId) { writer ->
                sendFileMeta(writer, metadata)
                sendFileChunks(writer, targetFile, message.id)
            }.onFailure {
                // 失败后回滚引用计数
                val toDelete = fileReferenceManager.release(file.absolutePath)
                toDelete?.let { privateFileManager.deleteFile(it) }
            }.getOrThrow()
        }

        updateStatus(
            messageId = message.id,
            sessionId = message.receiverId,
            status = SendStatus.Sent
        )
    }.onFailure { e ->
        handleSendError(message.id, message.receiverId, e)
        throw e
    }

    /**
     * 文件去重：checksum 已存在则复用原文件，否则注册新文件
     * 返回实际用于传输的目标文件
     */
    private suspend fun resolveTargetFile(message: MessageEntity, file: File): File {
        val checksum = file.toSHA256Hex()
        val existingFile = mediaFileDao.getByChecksum(checksum)

        return if (existingFile != null) {
            fileReferenceManager.retain(existingFile.localPath, checksum)
            messageDao.update(message.id) { it.copy(localPath = existingFile.localPath) }
            privateFileManager.deleteFile(file.absolutePath)
            File(existingFile.localPath)
        } else {
            fileReferenceManager.retain(file.absolutePath, checksum)
            file
        }
    }

    /**
     * 构建已签名的媒体消息元数据
     */
    private suspend fun buildSignedMetadata(
        message: MessageEntity,
        targetFile: File
    ): ChatProtocol.MediaMessage {
        val checksum = targetFile.toSHA256Hex()
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
        val signature = packetSigner.sign(unsigned, localIdentity.getPrivateKey())
        return unsigned.copy(signature = signature)
    }

    private fun sendFileMeta(
        writer: EncryptingPacketWriter,
        metadata: ChatProtocol.MediaMessage
    ) {
        writer.write(
            Packet(
                type = PacketType.FILE_META,
                body = serializeMediaMeta(metadata)
            )
        )
    }

    private suspend fun sendFileChunks(
        writer: EncryptingPacketWriter,
        file: File,
        messageId: String
    ) {
        streamFileChunks(file, file.length(), messageId) { chunk ->
            writer.writeNoFlush(Packet(PacketType.FILE_CHUNK, chunk))
        }
    }

    /**
     * 发送回执消息
     */
    suspend fun sendReceipt(messageId: String, receiverId: String, type: ReceiptType) {
        val protocol = ChatProtocol.MessageReceipt(
            messageId = messageId,
            senderId = myUserId,
            receiverId = receiverId,
            receiptType = type,
            signature = "",
            timestamp = System.currentTimeMillis()
        )
        val signature = packetSigner.sign(protocol, localIdentity.getPrivateKey())

        val packet = Packet(
            PacketType.RECEIPT,
            serializePolymorphic(protocol.copy(signature = signature))
        )

        transport.send(receiverId, packet)
            .onFailure {
                Log.w(TAG, "回执发送失败: $receiverId")
            }
    }

    /**
     * 发送个人资料给对方
     */
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
        val signature = packetSigner.sign(protocol, localIdentity.getPrivateKey())

        val packet = Packet(
            type = PacketType.PROFILE_RESPONSE,
            body = json.encodeToString<ChatProtocol>(
                protocol.copy(signature = signature)
            ).toByteArray(Charsets.UTF_8)
        )

        transport.send(userId, packet)
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
        val buffer = ByteArray(fileChunkSize)
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
                if (fileSize > 0 && totalSent - lastReportedAt >= progressInterval) {
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
        val failReason = if (error is ConnectionException) {
            error.failReason
        } else {
            if (error is CancellationException) {
                SendError.Cancelled
            } else {
                SendError.Unknown
            }
        }

        database.withTransaction {
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
        if (error !is CancellationException) {
            connectionInfoDao.markOffline(receiverId)
        }
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