package top.chengdongqing.wechat.data.network.messaging

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.designsystem.util.isTrue
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.core.util.toMD5Hex
import top.chengdongqing.wechat.data.model.PermissionResult
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.connection.ChatTransportManager
import top.chengdongqing.wechat.data.network.connection.ConnectionEvent
import top.chengdongqing.wechat.data.network.connection.PeerConnection
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketType
import top.chengdongqing.wechat.data.network.protocol.ReceiptType
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息接收器
 *
 * 负责从 SocketServer/SocketClient 的 receiveChannel 消费 Packet，
 * 路由到对应处理器后交给 [MessageDispatcher]。
 *
 * 媒体接收设计：
 * - FILE_META 到达时创建临时文件和 [MediaReceiveState]
 * - FILE_CHUNK 逐片追加写入磁盘，峰值内存仅一个 chunk（256KB）
 * - 全部 chunk 到齐后做 MD5 校验，通过后移交 [MessageDispatcher] 持久化
 * - BufferedOutputStream 256KB 写缓冲，与 chunk 大小对齐，减少磁盘 syscall
 */
@Singleton
class MessageReceiver @Inject constructor(
    private val transport: ChatTransportManager,
    private val messageSender: MessageSender,
    private val messageDispatcher: MessageDispatcher,
    private val contactRepository: ContactRepository,
    private val json: Json,
    @param:IoScope private val scope: CoroutineScope,
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "MessageReceiver"

        /** 磁盘写缓冲，与 FILE_CHUNK 大小对齐 */
        const val DISK_WRITE_BUFFER = 256 * 1024
    }

    val incomingMessageFlow = messageDispatcher.incomingMessageFlow

    private val mediaStates = mutableMapOf<String, MediaReceiveState>()

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
        scope.launch {
            consumePackets(conn.userId, conn.receiveChannel)
        }
    }

    /**
     * 循环消费 channel 中的 Packet，channel 关闭时退出并清理媒体状态
     */
    private suspend fun consumePackets(userId: String, channel: Channel<Packet>) {
        try {
            for (packet in channel) {
                handlePacket(userId, packet)
            }
        } catch (e: Exception) {
            Log.e(TAG, "消费异常: $userId", e)
        } finally {
            cleanupMediaState(userId)
            Log.w(TAG, "连接已关闭: $userId")
        }
    }

    /**
     * 按 PacketType 路由到对应处理器
     *
     * FILE_CHUNK 异常时额外清理媒体状态
     */
    private suspend fun handlePacket(userId: String, packet: Packet) {
        try {
            when (packet.type) {
                PacketType.FILE_META -> handleFileMeta(userId, packet.body)
                PacketType.FILE_CHUNK -> handleFileChunk(userId, packet.body)
                else -> handleJsonPacket(userId, packet)
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理 Packet 失败 (userId=$userId, type=${packet.type})", e)
            if (packet.type == PacketType.FILE_CHUNK) cleanupMediaState(userId)
        }
    }

    /**
     * 反序列化 JSON 包并交给 MessageDispatcher 分发
     */
    private suspend fun handleJsonPacket(userId: String, packet: Packet) {
        val protocol = json.decodeFromString<ChatProtocol>(String(packet.body, Charsets.UTF_8))

        // 拦截非好友/已拉黑（在非回执包时判断）
        if (protocol !is ChatProtocol.MessageReceipt
            && !canProcessMessage(userId, protocol.messageId)
        ) return

        messageDispatcher.dispatch(protocol)
    }

    /**
     * 校验发送者权限：非好友或被拉黑则发送拒收回执 并返回 false
     */
    private suspend fun canProcessMessage(userId: String, messageId: String): Boolean {
        return when (checkMessagePermission(userId)) {
            PermissionResult.Blocked -> {
                messageSender.sendReceipt(messageId, userId, ReceiptType.Blocked)
                false
            }

            PermissionResult.NotFriend -> {
                messageSender.sendReceipt(messageId, userId, ReceiptType.NotFriend)
                false
            }

            else -> true
        }
    }

    private suspend fun checkMessagePermission(userId: String): PermissionResult {
        val contact = contactRepository.getContact(userId)
        return when {
            contact?.isBlocked.isTrue() -> PermissionResult.Blocked
            contact == null -> PermissionResult.NotFriend
            else -> PermissionResult.Allowed
        }
    }

    /**
     * 处理媒体元数据包
     *
     * 创建临时文件和 [MediaReceiveState]，为后续 FILE_CHUNK 做好准备。
     * 若上一个媒体传输未完成则先清理，防止状态泄漏。
     */
    private suspend fun handleFileMeta(userId: String, body: ByteArray) {
        cleanupMediaState(userId)

        val metadata = json.decodeFromString<ChatProtocol.MediaMessage>(
            String(body, Charsets.UTF_8)
        )

        // 拦截非好友/已拉黑
        if (!canProcessMessage(metadata.senderId, metadata.messageId)) return

        withContext(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "recv_${metadata.messageId}.tmp")
            val outputStream = BufferedOutputStream(FileOutputStream(tempFile), DISK_WRITE_BUFFER)

            mediaStates[userId] = MediaReceiveState(metadata, tempFile, outputStream)
            Log.d(TAG, "开始接收媒体: messageId=${metadata.messageId}, 大小=${metadata.fileSize}")
        }
    }

    /**
     * 处理媒体分片包
     *
     * 追加写入临时文件；达到预期大小后：
     * 1. MD5 校验（checksum 存在时）
     * 2. 校验通过则移交 [MessageDispatcher]，否则删除临时文件并等待重传
     */
    private suspend fun handleFileChunk(userId: String, chunkData: ByteArray) =
        withContext(Dispatchers.IO) {
            val state = mediaStates[userId] ?: run {
                Log.w(TAG, "收到 FILE_CHUNK 但无对应 FILE_META (userId=$userId)")
                return@withContext
            }

            state.outputStream.write(chunkData)
            state.receivedBytes += chunkData.size

            // 进度节流
            if (state.receivedBytes - state.lastReportedAt >= TransferConfig.PROGRESS_REPORT_INTERVAL) {
                state.lastReportedAt = state.receivedBytes
            }

            if (state.receivedBytes < state.metadata.fileSize) return@withContext

            // 全部分片已到齐
            state.outputStream.flush()
            state.outputStream.close()

            // MD5 校验
            val expectedChecksum = state.metadata.checksum
            if (!expectedChecksum.isNullOrEmpty()) {
                val actualChecksum = state.tempFile.toMD5Hex()
                if (actualChecksum != expectedChecksum) {
                    Log.e(
                        TAG,
                        "MD5 校验失败 [${state.metadata.messageId}]: 期望=$expectedChecksum, 实际=$actualChecksum"
                    )
                    state.tempFile.delete()
                    mediaStates.remove(userId)
                    // TODO 通知发送端重传
                    return@withContext
                }
            }

            messageDispatcher.dispatch(state.metadata, state.tempFile)
            mediaStates.remove(userId)
        }

    /**
     * 清理未完成的媒体接收状态，关闭流并删除临时文件
     */
    private fun cleanupMediaState(userId: String) {
        mediaStates.remove(userId)?.let {
            it.cleanup()
            Log.w(TAG, "已清理未完成的媒体接收: messageId=${it.metadata.messageId}")
        }
    }
}

/**
 * 单个媒体文件的接收状态
 *
 * 以 userId 为 key 存储，同一连接同一时刻只能接收一个媒体文件。
 */
private class MediaReceiveState(
    val metadata: ChatProtocol.MediaMessage,
    val tempFile: File,
    val outputStream: BufferedOutputStream,
    var receivedBytes: Long = 0,
    var lastReportedAt: Long = 0
) {
    fun cleanup() {
        runCatching { outputStream.close() }
        runCatching { tempFile.delete() }
    }
}