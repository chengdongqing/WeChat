package top.chengdongqing.wechat.data.network.connection.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.network.connection.ConnectionEvent
import top.chengdongqing.wechat.data.network.connection.PeerConnection
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketReader
import top.chengdongqing.wechat.data.network.protocol.PacketType
import top.chengdongqing.wechat.data.network.protocol.PacketWriter
import top.chengdongqing.wechat.features.settings.domain.repository.ChatSettingsRepository

/**
 * RFCOMM 出站连接管理器
 */
@Singleton
class SocketClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
    private val e2e: E2ESessionManager,
    private val connectionManager: ConnectionManager,
    private val chatSettingsRepository: ChatSettingsRepository,
) {
    companion object {
        private const val TAG = "RfcommClient"
    }

    private suspend fun isE2eEnabled() = chatSettingsRepository.e2eEnabled.first()

    @SuppressLint("MissingPermission")
    suspend fun connect(
        userId: String,
        macAddress: String,
        myUserId: String
    ): Result<PeerConnection> = withContext(Dispatchers.IO) {
        runCatching {
            val socket = createSocket(macAddress)
            val conn = PeerConnection(
                userId = userId,
                reader = PacketReader(socket.inputStream),
                writer = PacketWriter(socket.outputStream),
                isActiveProvider = { socket.isConnected },
                closeAction = { socket.close() }
            )

            // 保存连接
            connectionManager.register(conn)
            // 推送连接成功事件
            connectionManager.emitEvent(ConnectionEvent.Connected(userId, conn))

            // 发握手包
            sendHandshake(conn, myUserId)
            // 开始收包
            connectionManager.startReceiving(conn) { packet ->
                handleE2EInHandshake(conn, packet)
            }
            // 开始心跳
            connectionManager.startHeartbeat(conn)

            conn
        }.onFailure {
            Log.e(TAG, "连接失败: $userId", it)
            connectionManager.emitEvent(ConnectionEvent.Disconnected(userId, it.message))
        }
    }

    private fun createSocket(macAddress: String): BluetoothSocket {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE)
                as BluetoothManager).adapter
        val device = adapter.getRemoteDevice(macAddress)
        return device.createRfcommSocketToServiceRecord(SocketServer.RFCOMM_UUID).apply {
            connect()
        }
    }

    private suspend fun sendHandshake(conn: PeerConnection, myUserId: String) {
        e2e.removeSession(conn.userId)

        val e2eKey = if (isE2eEnabled()) e2e.prepareHandshake(conn.userId) else null
        val hs = ChatProtocol.Handshake(senderId = myUserId, e2ePublicKey = e2eKey)
        val body = json.encodeToString<ChatProtocol>(hs).toByteArray(Charsets.UTF_8)

        conn.writer.write(Packet(PacketType.HANDSHAKE, body))
    }

    private fun handleE2EInHandshake(conn: PeerConnection, packet: Packet) {
        runCatching {
            val hs = json.decodeFromString<ChatProtocol.Handshake>(
                String(packet.body, Charsets.UTF_8)
            )
            hs.e2ePublicKey?.let { peerKey ->
                val myKey = e2e.acceptHandshake(conn.userId, peerKey)
                val ack = ChatProtocol.Handshake(senderId = conn.userId, e2ePublicKeyAck = myKey)
                conn.writer.write(
                    Packet(
                        PacketType.HANDSHAKE,
                        json.encodeToString<ChatProtocol>(ack).toByteArray(Charsets.UTF_8)
                    )
                )
            }
            hs.e2ePublicKeyAck?.let { peerKey ->
                e2e.completeHandshake(conn.userId, peerKey)
            }
        }.onFailure {
            Log.e(TAG, "E2E 握手失败: ${conn.userId}", it)
        }
    }
}