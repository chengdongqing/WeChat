package top.chengdongqing.wechat.data.network.socket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.data.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketReader
import top.chengdongqing.wechat.data.network.protocol.PacketType
import top.chengdongqing.wechat.data.network.protocol.PacketWriter
import java.io.EOFException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TCP 出站连接管理器
 *
 * 负责主动发起连接、E2E 握手、收包路由和心跳保活。
 *
 * 性能设计：
 * - Socket 收发缓冲区 512KB，匹配 LAN 带宽
 * - tcpNoDelay = true，禁用 Nagle 避免 40ms 延迟
 * - [sendAtomicTransfer] 通过 Mutex 保证 FILE_META + FILE_CHUNK 序列原子
 * - 心跳引用计数（[SocketConnection.activeTransferCount]），文件传输中自动暂停心跳
 */
@Singleton
class SocketClient @Inject constructor(
    private val json: Json,
    private val e2e: E2ESessionManager,
    @param:IoScope private val scope: CoroutineScope
) {
    private companion object {
        const val TAG = "SocketClient"
    }

    private val activeConnections = ConcurrentHashMap<String, SocketConnection>()

    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>()

    /** 连接状态事件流，[top.chengdongqing.wechat.data.network.service.modules.ChatModule] 订阅后启动包监听 */
    val connectionEvents: Flow<ConnectionEvent> = _connectionEvents.asSharedFlow()

    // ==================== 公开接口 ====================

    /**
     * 主动连接指定用户
     *
     * 若已有旧连接先关闭，再建新连接。
     * 成功后依次：发握手 → 启动收包 → 启动心跳 → 发出 Connected 事件。
     */
    suspend fun connect(
        userId: String,
        host: String,
        port: Int,
        myUserId: String
    ): Result<SocketConnection> = withContext(Dispatchers.IO) {
        runCatching {
            closeExisting(userId)
            Log.d(TAG, "正在连接: $host:$port")

            val socket = createSocket(host, port)
            val connection = SocketConnection(
                userId = userId,
                socket = socket,
                reader = PacketReader(socket.getInputStream()),
                writer = PacketWriter(socket.getOutputStream())
            )
            activeConnections[userId] = connection

            sendHandshake(connection, myUserId)
            startReceiving(connection)
            startHeartbeat(connection)

            _connectionEvents.emit(ConnectionEvent.Connected(userId))
            Log.d(TAG, "连接成功: $userId")
            connection
        }.onFailure {
            Log.e(TAG, "连接失败: $userId", it)
            _connectionEvents.emit(ConnectionEvent.Disconnected(userId, it.message))
        }
    }

    /**
     * 发送单个 Packet
     *
     * 加密后写入并立即 flush，失败时断开连接。
     */
    suspend fun send(userId: String, packet: Packet): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireConnection(userId).writer.write(e2e.encryptPacket(userId, packet))
            }.onFailure {
                Log.e(TAG, "发送失败: $userId", it)
                disconnect(userId)
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

    /** 主动断开指定连接，释放资源并发出 Disconnected 事件 */
    suspend fun disconnect(userId: String) = withContext(Dispatchers.IO) {
        activeConnections.remove(userId)?.close()
        _connectionEvents.emit(ConnectionEvent.Disconnected(userId, "主动断开"))
        Log.d(TAG, "已断开: $userId")
    }

    /** 获取指定用户的连接实例，不存在返回 null */
    fun getConnection(userId: String): SocketConnection? = activeConnections[userId]

    /** 检查指定用户是否有活跃的出站连接 */
    fun isConnected(userId: String): Boolean = getConnection(userId)?.isActive == true

    /** 关闭所有出站连接，应用退出时调用 */
    fun closeAll() {
        activeConnections.values.forEach { it.close() }
        activeConnections.clear()
    }

    // ==================== 内部逻辑 ====================

    private fun requireConnection(userId: String): SocketConnection =
        getConnection(userId) ?: throw IllegalStateException("未找到连接: $userId")

    private fun closeExisting(userId: String) {
        activeConnections.remove(userId)?.close()
    }

    /**
     * 创建并配置 Socket
     *
     * 缓冲区必须在 connect 前设置，才能影响 TCP 窗口协商。
     * soTimeout = 0：不用读超时，由 Ping-Pong 判断连接存活。
     */
    private fun createSocket(host: String, port: Int): Socket = Socket().apply {
        sendBufferSize = TransferConfig.SOCKET_SEND_BUFFER
        receiveBufferSize = TransferConfig.SOCKET_RECV_BUFFER
        connect(InetSocketAddress(host, port), TransferConfig.CONNECT_TIMEOUT)
        soTimeout = 0
        keepAlive = true
        tcpNoDelay = true
    }

    /**
     * 发送握手包
     *
     * 重连场景先清除旧 E2E session，生成新密钥对后随握手包发出公钥。
     */
    private fun sendHandshake(connection: SocketConnection, myUserId: String) {
        e2e.removeSession(connection.userId)
        val e2eKey = e2e.prepareHandshake(connection.userId)
        val body = json.encodeToString<ChatProtocol>(
            ChatProtocol.Handshake(senderId = myUserId, e2ePublicKey = e2eKey)
        ).toByteArray(Charsets.UTF_8)
        connection.writer.write(Packet(PacketType.HANDSHAKE, body))
        Log.d(TAG, "握手包已发送: userId=${connection.userId} e2e=${e2eKey.take(20)}...")
    }

    /**
     * 处理握手包中的 E2E 密钥交换
     *
     * e2ePublicKey 非空    → 被动方：派生 session key，回传公钥 ACK
     * e2ePublicKeyAck 非空 → 主动方：用暂存私钥完成派生，握手结束
     * 两者均空             → 普通握手，无需 E2E 处理
     */
    private fun handleE2EInHandshake(connection: SocketConnection, packet: Packet) {
        runCatching {
            val hs = json.decodeFromString<ChatProtocol.Handshake>(
                String(packet.body, Charsets.UTF_8)
            )
            hs.e2ePublicKey?.let { peerKey ->
                val myKey = e2e.acceptHandshake(connection.userId, peerKey)
                val ack =
                    ChatProtocol.Handshake(senderId = connection.userId, e2ePublicKeyAck = myKey)
                connection.writer.write(
                    Packet(
                        PacketType.HANDSHAKE,
                        json.encodeToString<ChatProtocol>(ack).toByteArray(Charsets.UTF_8)
                    )
                )
                Log.d(TAG, "E2E ACK 已回传: ${connection.userId}")
            }
            hs.e2ePublicKeyAck?.let { peerKey ->
                e2e.completeHandshake(connection.userId, peerKey)
                Log.d(TAG, "E2E 握手完成: ${connection.userId}")
            }
        }.onFailure {
            Log.e(TAG, "E2E 握手处理失败: ${connection.userId}", it)
        }
    }

    /**
     * 收包循环
     *
     * PONG      → 更新心跳时间戳
     * PING      → 回复 PONG
     * HANDSHAKE → E2E 密钥交换，再转发给 MessageReceiver
     * 其他       → 解密后推入 receiveChannel
     *
     * EOFException 表示对端正常关闭，其他异常打 error 日志。
     * 无论何种退出原因，finally 里统一断开连接并清理资源。
     */
    private fun startReceiving(connection: SocketConnection) {
        scope.launch {
            try {
                while (connection.isActive) {
                    val raw = connection.reader.read()
                    when (raw.type) {
                        PacketType.PONG -> connection.lastPongTime.set(System.currentTimeMillis())
                        PacketType.PING -> connection.writer.write(Packet.pong())
                        PacketType.HANDSHAKE -> {
                            handleE2EInHandshake(connection, raw)
                            connection.receiveChannel.send(raw)
                        }

                        else -> connection.receiveChannel.send(
                            e2e.decryptPacket(
                                connection.userId,
                                raw
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is EOFException, is SocketException -> {
                        Log.d(TAG, "接收中断: ${connection.userId}, ${e.message}")
                    }

                    else -> Log.e(TAG, "接收中断: ${connection.userId}", e)
                }
            } finally {
                disconnect(connection.userId)
            }
        }
    }

    /**
     * 心跳保活
     *
     * 每隔 [TransferConfig.PING_INTERVAL] 发一次 Ping。
     * 超过 [TransferConfig.PONG_TIMEOUT] 未收到 Pong 则主动断开。
     * 文件传输中（[SocketConnection.activeTransferCount] > 0）跳过，
     * 传输结束时 [SocketConnection.decrementTransferCount] 会重置 lastPongTime 防止误判。
     */
    private fun startHeartbeat(connection: SocketConnection) {
        connection.lastPongTime.set(System.currentTimeMillis())
        connection.heartbeatJob = scope.launch {
            try {
                while (connection.isActive) {
                    delay(TransferConfig.PING_INTERVAL)
                    if (connection.activeTransferCount.get() > 0) continue

                    val elapsed = System.currentTimeMillis() - connection.lastPongTime.get()
                    if (elapsed > TransferConfig.PONG_TIMEOUT) {
                        Log.w(TAG, "Pong 超时 (${elapsed}ms)，断开: ${connection.userId}")
                        disconnect(connection.userId)
                        break
                    }

                    runCatching { connection.writer.write(Packet.ping()) }
                        .onFailure {
                            Log.e(TAG, "Ping 失败: ${connection.userId}", it)
                            disconnect(connection.userId)
                            break
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "心跳异常: ${connection.userId}", e)
            }
        }
    }
}

// ==================== 数据类 ====================

/**
 * 出站连接实例
 *
 * @param transferMutex      文件传输时加锁，保证 META+CHUNK 序列原子不被插入
 * @param activeTransferCount 传输引用计数，> 0 时心跳自动暂停
 * @param lastPongTime       最后收到 Pong 的时间戳，超时判断依据
 */
data class SocketConnection(
    val userId: String,
    val socket: Socket,
    val reader: PacketReader,
    val writer: PacketWriter,
    val receiveChannel: Channel<Packet> = Channel(Channel.UNLIMITED),
    val transferMutex: Mutex = Mutex(),
    val activeTransferCount: AtomicInteger = AtomicInteger(0),
    val lastPongTime: AtomicLong = AtomicLong(System.currentTimeMillis()),
    var heartbeatJob: Job? = null
) {
    val isActive: Boolean get() = socket.isConnected && !socket.isClosed

    /** 传输开始，心跳自动暂停 */
    fun incrementTransferCount() = activeTransferCount.incrementAndGet()

    /**
     * 传输结束
     *
     * 计数归零时重置 [lastPongTime]，避免传输期间心跳暂停积累的时间触发误判超时。
     */
    fun decrementTransferCount() {
        if (activeTransferCount.decrementAndGet() <= 0) {
            lastPongTime.set(System.currentTimeMillis())
        }
    }

    /** 关闭连接，取消心跳并释放所有 IO 资源 */
    fun close() {
        runCatching {
            heartbeatJob?.cancel()
            receiveChannel.close()
            reader.close()
            writer.close()
            socket.close()
        }
    }
}

sealed class ConnectionEvent {
    /** TCP 连接已建立并完成握手 */
    data class Connected(val userId: String) : ConnectionEvent()

    /** 连接已断开，[reason] 说明原因（主动断开 / Pong 超时 / 接收异常等） */
    data class Disconnected(val userId: String, val reason: String?) : ConnectionEvent()
}