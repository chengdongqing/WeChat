package top.chengdongqing.wechat.data.network.connection

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.model.SendError
import top.chengdongqing.wechat.data.network.config.TransferConfig.PING_INTERVAL
import top.chengdongqing.wechat.data.network.config.TransferConfig.PONG_TIMEOUT
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.data.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.data.network.model.Packet
import top.chengdongqing.wechat.data.network.model.PacketType
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

/**
 * 连接管理器基类
 *
 * 提供连接池维护、收发消息、心跳等通用能力。
 */
abstract class ConnectionManager(
    protected open val e2e: E2ESessionManager,
    protected open val connectionInfoDao: ConnectionInfoDao,
    protected open val profileRepository: ProfileRepository,
    protected open val contactRepository: ContactRepository,
    protected open val scope: CoroutineScope
) {
    val connections = ConcurrentHashMap<String, PeerConnection>()

    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 8)
    val connectionEvents = _connectionEvents.asSharedFlow()

    fun isConnected(userId: String) = connections[userId]?.isActive == true

    fun requireConnection(userId: String) =
        connections[userId] ?: throw ConnectionException(
            "未找到连接: $userId",
            SendError.ConnectionFailed
        )

    open suspend fun register(conn: PeerConnection) {
        connections[conn.userId]?.close()
        connections[conn.userId] = conn
        connectionInfoDao.markOnline(conn.userId, conn.lastPongTime.get())
    }

    fun close(userId: String) = connections.remove(userId)?.close()

    fun closeAll() {
        connections.values.forEach { it.close() }
        connections.clear()
    }

    suspend fun emitEvent(event: ConnectionEvent) = _connectionEvents.emit(event)

    suspend fun send(userId: String, packet: Packet): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = requireConnection(userId)
            conn.writer.write(e2e.encryptPacket(userId, packet))
            // 有消息往来，重置空闲计时
            conn.lastPongTime.set(System.currentTimeMillis())
        }.onFailure {
            Log.e(tag, "发送失败: $userId", it)
            disconnect(userId)
        }
    }

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
            Log.e(tag, "原子传输失败: $userId", e)
            // 非用户主动取消才需要断开连接
            if (e !is CancellationException) {
                disconnect(userId)
            }
            Result.failure(e)
        } finally {
            conn.decrementTransferCount()
            conn.transferMutex.unlock()
        }
    }

    suspend fun disconnect(userId: String) = withContext(Dispatchers.IO) {
        close(userId)
        e2e.removeSession(userId)
        connectionInfoDao.markOffline(userId)
        emitEvent(ConnectionEvent.Disconnected(userId, "主动断开"))
    }

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

                    runCatching {
                        // 携带个人资料版本号
                        val profileVersion = profileRepository.requireProfile().updatedAt
                        conn.writer.write(Packet.ping(profileVersion))
                    }.onFailure {
                        throw ConnectionException(
                            "Ping 失败",
                            SendError.ConnectionFailed
                        )
                    }
                }
            } catch (_: Exception) {
                Log.w(tag, "心跳异常，断开: ${conn.userId}")
                // 断开连接
                disconnect(conn.userId)
                // 标记离线
                connectionInfoDao.markOffline(conn.userId)
            }
        }
    }

    fun startReceiving(conn: PeerConnection, onHandshake: ((Packet) -> Unit)? = null) {
        scope.launch {
            try {
                while (conn.isActive) {
                    val packet = conn.reader.read()
                    val userId = conn.userId

                    when (packet.type) {
                        PacketType.PING -> {
                            // 回应心跳
                            conn.writer.write(Packet.pong())
                            // 检查对方的资料是否需要更新
                            checkProfileVersion(userId, packet)
                        }

                        PacketType.PONG -> conn.lastPongTime.set(System.currentTimeMillis())
                        PacketType.HANDSHAKE -> onHandshake?.invoke(packet)
                        else -> conn.receiveChannel.send(e2e.decryptPacket(userId, packet))
                    }
                }
            } catch (_: Exception) {
                Log.w(tag, "接收异常: ${conn.userId}")
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
            // 发送拉取个人资料请求
            send(
                userId = userId,
                packet = Packet(PacketType.PROFILE_REQUEST)
            )
        }
    }

    protected abstract val tag: String
}