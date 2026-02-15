package top.chengdongqing.wechat.data.network.messaging

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.util.toMD5Hex
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketType
import top.chengdongqing.wechat.data.network.socket.ClientConnection
import top.chengdongqing.wechat.data.network.socket.SocketConnection
import top.chengdongqing.wechat.data.network.socket.SocketServer
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息接收器
 *
 * FILE_CHUNK 直接写磁盘临时文件，峰值内存仅一个 chunk (256KB)。
 * 使用 BufferedOutputStream 减少磁盘 write syscall。
 */
@Singleton
class MessageReceiver @Inject constructor(
    private val socketServer: SocketServer,
    private val dispatcher: MessageDispatcher,
    private val json: Json,
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "MessageReceiver"

        /** 磁盘写缓冲: 256KB，与 chunk 大小对齐 */
        const val DISK_WRITE_BUFFER = 256 * 1024
    }

    val incomingMessageFlow = dispatcher.incomingMessageFlow
    val signalingFlow = dispatcher.signalingFlow

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    private val mediaStates = mutableMapOf<String, MediaReceiveState>()

    // ==================== 公开接口 ====================

    fun start() {
        scope.launch {
            socketServer.incomingConnections.collect { incoming ->
                startListening(incoming.connection)
            }
        }
        Log.d(TAG, "消息接收器已启动")
    }

    fun startListening(connection: SocketConnection) {
        scope.launch { consumePackets(connection.userId, connection.receiveChannel) }
    }

    fun startListening(connection: ClientConnection) {
        scope.launch { consumePackets(connection.userId, connection.receiveChannel) }
    }

    // ==================== 核心逻辑 ====================

    private suspend fun consumePackets(
        userId: String,
        channel: Channel<Packet>
    ) {
        try {
            for (packet in channel) {
                handlePacket(userId, packet)
            }
        } catch (e: Exception) {
            Log.e(TAG, "消费异常: $userId", e)
        } finally {
            cleanupMediaState(userId)
            Log.d(TAG, "连接已关闭: $userId")
        }
    }

    private suspend fun handlePacket(userId: String, packet: Packet) {
        try {
            when (packet.type) {
                PacketType.FILE_META -> handleFileMeta(userId, packet.body)
                PacketType.FILE_CHUNK -> handleFileChunk(userId, packet.body)
                else -> handleJsonPacket(packet)
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理 Packet 失败 (userId=$userId, type=${packet.type})", e)
            if (packet.type == PacketType.FILE_CHUNK) {
                cleanupMediaState(userId)
            }
        }
    }

    private suspend fun handleJsonPacket(packet: Packet) {
        val jsonString = String(packet.body, Charsets.UTF_8)
        val protocol = json.decodeFromString<ChatProtocol>(jsonString)
        dispatcher.dispatch(protocol)
    }

    private fun handleFileMeta(userId: String, body: ByteArray) {
        cleanupMediaState(userId)

        val jsonString = String(body, Charsets.UTF_8)
        val metadata = json.decodeFromString<ChatProtocol.MediaMessage>(jsonString)

        val tempFile = File(context.cacheDir, "recv_${metadata.messageId}.tmp")
        val outputStream = BufferedOutputStream(FileOutputStream(tempFile), DISK_WRITE_BUFFER)

        mediaStates[userId] = MediaReceiveState(metadata, tempFile, outputStream)
        Log.d(TAG, "开始接收媒体: messageId=${metadata.messageId}, 大小=${metadata.fileSize}")
    }

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
                val percent = (state.receivedBytes * 100) / state.metadata.fileSize
                Log.d(TAG, "接收 [${state.metadata.messageId}]: $percent%")
            }

            // 接收完毕
            if (state.receivedBytes >= state.metadata.fileSize) {
                state.outputStream.flush()
                state.outputStream.close()

                // MD5校验
                val expectedChecksum = state.metadata.checksum
                if (!expectedChecksum.isNullOrEmpty()) {
                    val actualChecksum = state.tempFile.toMD5Hex()

                    if (actualChecksum != expectedChecksum) {
                        Log.e(
                            TAG, "MD5 校验失败 [${state.metadata.messageId}]: " +
                                    "期望=$expectedChecksum, 实际=$actualChecksum"
                        )
                        state.tempFile.delete()
                        mediaStates.remove(userId)
                        // TODO 通知发送端重传
                        return@withContext
                    } else {
                        Log.d(
                            TAG, "MD5 校验成功 [${state.metadata.messageId}]: " +
                                    "期望=$expectedChecksum, 实际=$actualChecksum"
                        )
                    }
                }

                dispatcher.dispatch(state.metadata, state.tempFile)

                mediaStates.remove(userId)
                Log.d(TAG, "媒体接收完成: messageId=${state.metadata.messageId}")
            }
        }

    private fun cleanupMediaState(userId: String) {
        mediaStates.remove(userId)?.let {
            it.cleanup()
            Log.w(TAG, "清理未完成的媒体接收: messageId=${it.metadata.messageId}")
        }
    }
}