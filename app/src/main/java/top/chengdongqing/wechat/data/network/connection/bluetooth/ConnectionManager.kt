package top.chengdongqing.wechat.data.network.connection.bluetooth

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.model.SendError
import top.chengdongqing.wechat.data.network.config.TransferConfig.PING_INTERVAL
import top.chengdongqing.wechat.data.network.config.TransferConfig.PONG_TIMEOUT
import top.chengdongqing.wechat.data.network.connection.ConnectionEvent
import top.chengdongqing.wechat.data.network.connection.ConnectionException
import top.chengdongqing.wechat.data.network.connection.PeerConnection
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.data.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketType
import java.io.EOFException
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionManager @Inject constructor(
    private val e2e: E2ESessionManager,
    private val connectionInfoDao: ConnectionInfoDao,
    @param:IoScope private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "BtConnectionManager"

        /** 无消息超过此时间后自动断开，节省蓝牙资源 */
        private const val IDLE_TIMEOUT_MS = 5 * 60 * 1000L
    }

    val connections = ConcurrentHashMap<String, PeerConnection>()

    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 8)
    val connectionEvents = _connectionEvents.asSharedFlow()

    fun isConnected(userId: String) = connections[userId]?.isActive == true

    fun requireConnection(userId: String) =
        connections[userId] ?: throw ConnectionException(
            "未找到连接: $userId",
            SendError.ConnectionFailed
        )

    suspend fun register(conn: PeerConnection) {
        connections[conn.userId]?.close()
        connections[conn.userId] = conn
        startIdleTimer(conn)
        // 标记在线
        connectionInfoDao.markOnline(conn.userId, System.currentTimeMillis())
    }

    fun close(userId: String) {
        connections.remove(userId)?.close()
    }

    fun closeAll() {
        connections.values.forEach { it.close() }
        connections.clear()
    }

    suspend fun emitEvent(event: ConnectionEvent) {
        _connectionEvents.emit(event)
    }

    suspend fun send(userId: String, packet: Packet): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = requireConnection(userId)
                conn.writer.write(e2e.encryptPacket(userId, packet))
                // 有消息往来，重置空闲计时
                conn.lastPongTime.set(System.currentTimeMillis())
            }.onFailure { e ->
                Log.e(TAG, "发送失败: $userId", e)
                disconnect(userId)
                throw e
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
            Log.e(TAG, "原子传输失败: $userId", e)
            disconnect(userId)
            Result.failure(e)
        } finally {
            conn.decrementTransferCount()
            conn.transferMutex.unlock()
        }
    }

    suspend fun disconnect(userId: String) = withContext(Dispatchers.IO) {
        close(userId)
        e2e.removeSession(userId)
        emitEvent(ConnectionEvent.Disconnected(userId, "主动断开"))
    }

    fun startHeartbeat(conn: PeerConnection) {
        conn.lastPongTime.set(System.currentTimeMillis())
        conn.heartbeatJob = scope.launch {
            try {
                while (conn.isActive) {
                    delay(PING_INTERVAL)
                    if (conn.activeTransferCount.get() > 0) continue
                    val elapsed = System.currentTimeMillis() - conn.lastPongTime.get()
                    if (elapsed > PONG_TIMEOUT) {
                        throw ConnectionException("Pong 超时", SendError.ConnectionFailed)
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
                disconnect(conn.userId)
            }
        }
    }

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
                            val decrypted = e2e.decryptPacket(conn.userId, packet)

                            if (decrypted.body.isNotEmpty()) {
                                // 推送到处理队列
                                conn.receiveChannel.send(decrypted)
                            } else {
                                Log.w(TAG, "解密后 body 为空，丢弃: ${conn.userId}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is EOFException, is SocketException -> Log.w(TAG, "接收中断: ${conn.userId}")
                    else -> Log.e(TAG, "接收异常: ${conn.userId}", e)
                }
            } finally {
                e2e.removeSession(conn.userId)
                disconnect(conn.userId)
            }
        }
    }

    /**
     * 空闲超时自动断开
     * 蓝牙连接数有限，没有消息往来时主动释放
     */
    private fun startIdleTimer(conn: PeerConnection) {
        scope.launch {
            while (conn.isActive) {
                delay(IDLE_TIMEOUT_MS)
                val idle = System.currentTimeMillis() - conn.lastPongTime.get()
                if (idle >= IDLE_TIMEOUT_MS) {
                    Log.d(TAG, "连接空闲超时，断开: ${conn.userId}")
                    disconnect(conn.userId)
                    break
                }
            }
        }
    }
}