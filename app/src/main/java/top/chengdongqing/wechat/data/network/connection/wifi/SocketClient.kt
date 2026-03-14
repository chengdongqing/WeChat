package top.chengdongqing.wechat.data.network.connection.wifi

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.connection.ConnectionEvent
import top.chengdongqing.wechat.data.network.connection.PeerConnection
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketReader
import top.chengdongqing.wechat.data.network.protocol.PacketType
import top.chengdongqing.wechat.data.network.protocol.PacketWriter
import top.chengdongqing.wechat.features.settings.domain.repository.ChatSettingsRepository
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TCP 出站连接管理器
 *
 * 负责主动发起连接、E2E 握手、收包路由和心跳保活。
 */
@Singleton
class SocketClient @Inject constructor(
    private val json: Json,
    private val e2e: E2ESessionManager,
    private val connectionManager: ConnectionManager,
    private val chatSettingsRepository: ChatSettingsRepository
) {
    private companion object {
        const val TAG = "SocketClient"
    }

    private suspend fun isE2eEnabled(): Boolean =
        chatSettingsRepository.e2eEnabled.first()

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
    ): Result<PeerConnection> = withContext(Dispatchers.IO) {
        runCatching {
            val socket = createSocket(host, port)
            val conn = PeerConnection(
                userId = userId,
                reader = PacketReader(socket.getInputStream()),
                writer = PacketWriter(socket.getOutputStream()),
                isActiveProvider = { socket.isConnected && !socket.isClosed },
                closeAction = { socket.close() }
            )

            // 保存连接
            connectionManager.register(conn)
            // 推送连接成功事件
            connectionManager.emitEvent(ConnectionEvent.Connected(userId, conn))

            // 发送握手包
            sendHandshake(conn, myUserId)
            // 开始接收数据
            connectionManager.startReceiving(conn) { packet ->
                handleE2EInHandshake(conn, packet)
            }
            // 开始维持心跳
            connectionManager.startHeartbeat(conn)

            conn
        }.onFailure { e ->
            Log.e(TAG, "连接失败: $userId", e)
            connectionManager.emitEvent(ConnectionEvent.Disconnected(userId, e.message))
        }
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
    private suspend fun sendHandshake(connection: PeerConnection, myUserId: String) {
        e2e.removeSession(connection.userId)

        val e2eKey = if (isE2eEnabled()) e2e.prepareHandshake(connection.userId) else null
        val hs = ChatProtocol.Handshake(senderId = myUserId, e2ePublicKey = e2eKey)
        val body = json.encodeToString<ChatProtocol>(hs).toByteArray(Charsets.UTF_8)

        connection.writer.write(Packet(PacketType.HANDSHAKE, body))
    }

    /**
     * 处理握手包中的 E2E 密钥交换
     *
     * e2ePublicKey 非空    → 被动方：派生 session key，回传公钥 ACK
     * e2ePublicKeyAck 非空 → 主动方：用暂存私钥完成派生，握手结束
     * 两者均空             → 普通握手，无需 E2E 处理
     */
    private fun handleE2EInHandshake(connection: PeerConnection, packet: Packet) {
        runCatching {
            val hs = json.decodeFromString<ChatProtocol.Handshake>(
                String(packet.body, Charsets.UTF_8)
            )
            hs.e2ePublicKey?.let { peerKey ->
                val myKey = e2e.acceptHandshake(
                    peerId = connection.userId,
                    peerPublicKey = peerKey
                )
                val ack = ChatProtocol.Handshake(
                    senderId = connection.userId,
                    e2ePublicKeyAck = myKey
                )
                connection.writer.write(
                    Packet(
                        PacketType.HANDSHAKE,
                        json.encodeToString<ChatProtocol>(ack).toByteArray(Charsets.UTF_8)
                    )
                )
            }
            hs.e2ePublicKeyAck?.let { peerKey ->
                e2e.completeHandshake(connection.userId, peerKey)
            }
        }.onFailure {
            Log.e(TAG, "E2E 握手处理失败: ${connection.userId}", it)
        }
    }
}