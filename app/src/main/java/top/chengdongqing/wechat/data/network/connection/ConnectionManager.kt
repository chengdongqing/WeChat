package top.chengdongqing.wechat.data.network.connection

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.designsystem.util.isTrue
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.model.SendError
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.config.TransferConfig.PING_INTERVAL
import top.chengdongqing.wechat.data.network.config.TransferConfig.PONG_TIMEOUT
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.data.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketType
import java.io.EOFException
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 连接管理器
 */
@Singleton
class ConnectionManager @Inject constructor(
    private val e2e: E2ESessionManager,
    private val connectionInfoDao: ConnectionInfoDao,
    @param:IoScope private val scope: CoroutineScope
) {

    private companion object {
        const val TAG = "ConnectionManager"
    }

    // Socket 连接池
    private val connections = ConcurrentHashMap<String, PeerConnection>()

    // 连接状态事件流
    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>()
    val connectionEvents = _connectionEvents.asSharedFlow()

    /**
     * 获取指定连接
     */
    fun getConnection(userId: String) = connections[userId]

    /**
     * 获取指定连接
     * 连接不存在时抛出异常
     */
    fun requireConnection(userId: String) =
        getConnection(userId) ?: throw ConnectionException(
            "未找到连接: $userId",
            SendError.ConnectionFailed
        )

    /**
     * 是否已连接
     */
    fun isConnected(userId: String): Boolean = getConnection(userId)?.isActive.isTrue()

    /**
     * 注册连接
     */
    suspend fun register(conn: PeerConnection) {
        // 关闭可能已存在的连接
        connections[conn.userId]?.close()
        // 保存新连接
        connections[conn.userId] = conn
        // 标记在线
        connectionInfoDao.markOnline(conn.userId, System.currentTimeMillis())
    }

    /**
     * 关闭指定连接
     */
    fun close(userId: String) {
        connections.remove(userId)?.close()
    }

    /**
     * 关闭所有连接
     */
    fun closeAll() {
        connections.values.forEach { it.close() }
        connections.clear()
    }

    /**
     * 推送连接事件
     */
    suspend fun emitEvent(event: ConnectionEvent) {
        _connectionEvents.emit(event)
    }

    /**
     * 发送单个 Packet
     *
     * 加密后写入并立即 flush，失败时断开连接。
     */
    suspend fun send(userId: String, packet: Packet): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = requireConnection(userId)
                conn.writer.write(e2e.encryptPacket(userId, packet))
            }.onFailure { e ->
                Log.e(TAG, "发送失败: $userId", e)
                disconnect(userId)
                throw e
            }
        }

    /**
     * 原子发送一组 Packet（文件传输专用）
     *
     * [Mutex] 保证 FILE_META + FILE_CHUNK 序列不被其他传输插入。
     * block 内通过 [EncryptingPacketWriter.writeNoFlush] 批量写入，
     * block 结束后统一 flush，减少 syscall 次数。
     */
    suspend fun sendAtomicTransfer(
        userId: String,
        block: suspend (EncryptingPacketWriter) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val conn = requireConnection(userId)
        conn.transferMutex.lock()
        conn.incrementTransferCount()
        try {
            block(EncryptingPacketWriter(conn.writer, userId, e2e))
            conn.writer.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "原子传输失败: $userId", e)
            disconnect(userId)
            Result.failure(e)
        } finally {
            conn.decrementTransferCount()
            conn.transferMutex.unlock()
        }
    }

    /**
     * 断开指定连接
     */
    suspend fun disconnect(userId: String) = withContext(Dispatchers.IO) {
        close(userId)
        connectionInfoDao.markOffline(userId)
        emitEvent(ConnectionEvent.Disconnected(userId, "主动断开"))
    }

    /**
     * 心跳保活
     *
     * 每隔 [TransferConfig.PING_INTERVAL] 发一次 Ping。
     * 超过 [TransferConfig.PONG_TIMEOUT] 未收到 Pong 则主动断开。
     * 文件传输中（[PeerConnection.activeTransferCount] > 0）跳过，
     * 传输结束时 [PeerConnection.decrementTransferCount] 会重置 lastPongTime 防止误判。
     */
    fun startHeartbeat(conn: PeerConnection) {
        conn.lastPongTime.set(System.currentTimeMillis())
        conn.heartbeatJob = scope.launch {
            try {
                while (conn.isActive) {
                    delay(PING_INTERVAL)
                    if (conn.activeTransferCount.get() > 0) continue

                    val lastSeen = conn.lastPongTime.get()
                    val elapsed = System.currentTimeMillis() - lastSeen
                    if (elapsed > PONG_TIMEOUT) {
                        throw ConnectionException(
                            "Pong 超时 (${elapsed}ms)",
                            SendError.ConnectionFailed
                        )
                    } else {
                        // 标记在线
                        connectionInfoDao.markOnline(conn.userId, lastSeen)
                    }

                    runCatching { conn.writer.write(Packet.ping()) }
                        .onFailure {
                            throw ConnectionException(
                                "Ping 失败",
                                SendError.ConnectionFailed
                            )
                        }
                }
            } catch (_: Exception) {
                Log.w(TAG, "心跳异常，断开: ${conn.userId}")
                // 断开连接
                disconnect(conn.userId)
                // 标记离线
                connectionInfoDao.markOffline(conn.userId)
            }
        }
    }

    /**
     * 收包循环
     */
    fun startReceiving(conn: PeerConnection, onHandshake: ((Packet) -> Unit)? = null) {
        scope.launch {
            try {
                while (conn.isActive) {
                    val packet = conn.reader.read()
                    when (packet.type) {
                        PacketType.PING -> conn.writer.write(Packet.pong())
                        PacketType.PONG -> conn.lastPongTime.set(System.currentTimeMillis())
                        PacketType.HANDSHAKE -> onHandshake?.invoke(packet)
                        else -> {
                            // 解密数据包
                            val packet = e2e.decryptPacket(conn.userId, packet)
                            if (packet.body.isNotEmpty()) {
                                // 推送到处理队列
                                conn.receiveChannel.send(packet)
                            } else {
                                Log.w(TAG, "解密后 body 为空，丢弃: ${conn.userId}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is EOFException, is SocketException -> {
                        Log.w(TAG, "接收中断: ${conn.userId}")
                    }

                    else -> Log.e(TAG, "接收中断: ${conn.userId}", e)
                }
            } finally {
                e2e.removeSession(conn.userId)
                disconnect(conn.userId)
            }
        }
    }
}

/**
 * 连接事件
 */
sealed class ConnectionEvent {
    /**
     * TCP 连接已建立并完成握手
     */
    data class Connected(val userId: String, val conn: PeerConnection) : ConnectionEvent()

    /**
     * 连接已断开
     *
     * @param reason 说明原因（主动断开 / Pong 超时 / 接收异常等）
     */
    data class Disconnected(val userId: String, val reason: String?) : ConnectionEvent()
}