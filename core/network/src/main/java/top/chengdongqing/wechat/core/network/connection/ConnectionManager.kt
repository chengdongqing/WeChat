package top.chengdongqing.wechat.core.network.connection

import android.util.Log
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.common.di.IoScope
import top.chengdongqing.wechat.core.data.repository.ContactRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.core.model.SendError
import top.chengdongqing.wechat.core.network.config.TransferConfig
import top.chengdongqing.wechat.core.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.core.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.core.network.model.Packet
import top.chengdongqing.wechat.core.network.model.PacketType
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

/**
 * 连接管理器
 */
@Singleton
class ConnectionManager @Inject constructor(
    private val e2e: E2ESessionManager,
    private val connectionInfoDao: ConnectionInfoDao,
    private val profileRepository: ProfileRepository,
    private val contactRepository: ContactRepository,
    @param:IoScope private val scope: CoroutineScope
) {
    private companion object {
        const val TAG = "ConnectionManager"
    }

    val connections = ConcurrentHashMap<String, PeerConnection>()

    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 8)
    val connectionEvents = _connectionEvents.asSharedFlow()

    /**
     * 是否已连接
     */
    fun isConnected(userId: String) = connections[userId]?.isActive == true

    /**
     * 获取指定连接
     */
    fun requireConnection(userId: String) =
        connections[userId] ?: throw ConnectionException(
            "未找到连接: $userId",
            SendError.ConnectionFailed
        )

    /**
     * 注册连接
     */
    suspend fun register(conn: PeerConnection) {
        connections[conn.userId]?.close()
        connections[conn.userId] = conn
        connectionInfoDao.markOnline(conn.userId, conn.lastPongTime.get())
    }

    /**
     * 关闭指定连接
     */
    fun close(userId: String) = connections.remove(userId)?.close()

    /**
     * 关闭所有连接
     */
    fun closeAll() {
        connections.values.forEach { it.close() }
        connections.clear()
    }

    suspend fun emitEvent(event: ConnectionEvent) = _connectionEvents.emit(event)

    /**
     * 发送单个 Packet（控制包、文本消息等）。
     *
     * 通过 [PeerConnection.writeMutex] 保证写操作原子，不会与并发的文件分片交织。
     */
    suspend fun send(userId: String, packet: Packet): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = requireConnection(userId)
            val encrypted = e2e.encryptPacket(userId, packet)
            conn.writeMutex.withLock {
                conn.writer.write(encrypted)
            }
            // 有消息往来，重置空闲计时
            conn.lastPongTime.set(System.currentTimeMillis())
        }.onFailure {
            Log.e(TAG, "发送失败: $userId", it)
            disconnect(userId)
        }
    }

    /**
     * 文件传输入口
     *
     * - 通过 [PeerConnection.maxConcurrentTransfers] 限制同一连接的并发传输上限。
     *   超出上限的协程会挂起等待，直到有槽位释放。
     * - [block] 内部通过 [EncryptingPacketWriter] 写入分片；写入时自动持有
     *   [PeerConnection.writeMutex]，保证字节不与其他并发传输交织。
     * - 最终 flush 同样在锁内完成，确保缓冲区数据完整地提交到 Socket。
     *
     * @param userId 目标用户
     * @param block  传输逻辑，通过传入的 [EncryptingPacketWriter] 写包
     */
    suspend fun sendAtomicTransfer(
        userId: String,
        block: suspend (EncryptingPacketWriter) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val conn = requireConnection(userId)

        // 获取并发槽位（超限则挂起等待）
        conn.maxConcurrentTransfers.acquire()
        conn.incrementTransferCount()

        try {
            // writeMutex 注入 writer，每次 writeNoFlush 内部自动加锁
            val writer = EncryptingPacketWriter(conn.writer, userId, e2e, conn.writeMutex)
            block(writer)

            // 本次传输完毕后 flush，在锁内保证原子提交
            conn.writeMutex.withLock {
                conn.writer.flush()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "文件传输失败: $userId", e)
            // 非用户主动取消才需要断开连接
            if (e !is CancellationException) {
                disconnect(userId)
            }
            Result.failure(e)
        } finally {
            conn.decrementTransferCount()
            conn.maxConcurrentTransfers.release()   // 无论成败都释放槽位
        }
    }

    /**
     * 断开连接
     */
    suspend fun disconnect(userId: String) = withContext(Dispatchers.IO) {
        // 关闭连接
        close(userId)
        // 移除加密会话
        e2e.removeSession(userId)
        // 标记离线
        connectionInfoDao.markOffline(userId)
        // 推送事件
        emitEvent(ConnectionEvent.Disconnected(userId, "主动断开"))
    }

    /**
     * 开始心跳机制
     */
    fun startHeartbeat(conn: PeerConnection) {
        conn.lastPongTime.set(System.currentTimeMillis())
        conn.heartbeatJob = scope.launch {
            try {
                while (conn.isActive) {
                    delay(TransferConfig.PING_INTERVAL)
                    // 有文件传输时跳过心跳发送，但仍检测超时
                    if (conn.activeTransferCount.get() > 0) continue

                    val lastSeen = conn.lastPongTime.get()
                    val elapsed = System.currentTimeMillis() - lastSeen
                    if (elapsed > TransferConfig.PONG_TIMEOUT) {
                        throw ConnectionException(
                            "Pong 超时 (${elapsed}ms)",
                            SendError.ConnectionFailed
                        )
                    } else {
                        // 标记在线
                        connectionInfoDao.markOnline(conn.userId, lastSeen)
                    }

                    runCatching {
                        val profileVersion = profileRepository.requireProfile().updatedAt
                        // Ping 也走 writeMutex，避免和传输分片交织
                        conn.writeMutex.withLock {
                            conn.writer.write(Packet.ping(profileVersion))
                        }
                    }.onFailure {
                        throw ConnectionException("Ping 失败", SendError.ConnectionFailed)
                    }
                }
            } catch (_: Exception) {
                Log.w(TAG, "心跳异常，断开: ${conn.userId}")
                // 断开连接
                disconnect(conn.userId)
            }
        }
    }

    /**
     * 开始接收消息
     */
    fun startReceiving(conn: PeerConnection, onHandshake: ((Packet) -> Unit)? = null) {
        scope.launch {
            try {
                while (conn.isActive) {
                    val packet = conn.reader.read()
                    val userId = conn.userId

                    when (packet.type) {
                        PacketType.PING -> {
                            // 回应心跳
                            conn.writeMutex.withLock {
                                conn.writer.write(Packet.pong())
                            }
                            // 检查对方的个人资料是否需要更新
                            checkProfileVersion(userId, packet)
                        }

                        PacketType.PONG -> conn.lastPongTime.set(System.currentTimeMillis())
                        PacketType.HANDSHAKE -> onHandshake?.invoke(packet)
                        else -> conn.receiveChannel.send(e2e.decryptPacket(userId, packet))
                    }
                }
            } catch (_: Exception) {
                Log.w(TAG, "接收异常: ${conn.userId}")
            } finally {
                disconnect(conn.userId)
            }
        }
    }

    /**
     * 通过比对好友资料版本号，判断是否需要更新对方的资料
     */
    private suspend fun checkProfileVersion(userId: String, packet: Packet) {
        if (packet.body.size < 16) return // 长度不对时直接忽略

        val buffer = ByteBuffer.wrap(packet.body)
        val remoteProfileVersion = buffer.getLong() // 将自动取8个字节

        val contact = contactRepository.getContact(userId) ?: return
        if (remoteProfileVersion > contact.version) {
            // 发送拉取对方个人资料请求
            send(
                userId = userId,
                packet = Packet(PacketType.PROFILE_REQUEST)
            )
        }
    }
}