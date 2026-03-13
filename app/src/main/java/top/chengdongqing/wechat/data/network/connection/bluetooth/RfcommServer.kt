package top.chengdongqing.wechat.data.network.connection.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.network.connection.ConnectionEvent
import top.chengdongqing.wechat.data.network.connection.PeerConnection
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketReader
import top.chengdongqing.wechat.data.network.protocol.PacketType
import top.chengdongqing.wechat.data.network.protocol.PacketWriter
import java.util.UUID

/**
 * RFCOMM 入站连接管理器
 */
@Singleton
class RfcommServer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
    private val e2e: E2ESessionManager,
    private val connectionManager: BtConnectionManager,
    @param:IoScope private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "RfcommServer"
        val RFCOMM_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        private const val SERVICE_NAME = "WeChatChat"
    }

    private var serverSocket: BluetoothServerSocket? = null

    @SuppressLint("MissingPermission")
    suspend fun start() = withContext(Dispatchers.IO) {
        try {
            val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE)
                    as BluetoothManager).adapter
            serverSocket = adapter.listenUsingRfcommWithServiceRecord(
                SERVICE_NAME,
                RFCOMM_UUID
            )
            scope.launch { acceptLoop() }
            Log.d(TAG, "RFCOMM 服务已启动")
        } catch (e: Exception) {
            Log.e(TAG, "RFCOMM 服务启动失败", e)
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (_: Exception) {
            Log.d(TAG, "RFCOMM 服务已关闭")
        }
    }

    private fun acceptLoop() {
        val socket = serverSocket ?: return
        while (true) {
            try {
                val clientSocket = socket.accept()
                scope.launch { handleClient(clientSocket) }
            } catch (_: Exception) {
                Log.e(TAG, "接受连接异常，服务已停止")
                break
            }
        }
    }

    private suspend fun handleClient(socket: BluetoothSocket) = withContext(Dispatchers.IO) {
        try {
            val reader = PacketReader(socket.inputStream)
            val writer = PacketWriter(socket.outputStream)

            val userId = performHandshake(reader, writer) ?: run {
                Log.w(TAG, "握手失败")
                socket.close()
                return@withContext
            }

            val conn = PeerConnection(
                userId = userId,
                reader = reader,
                writer = writer,
                isActiveProvider = { socket.isConnected },
                closeAction = { socket.close() }
            )

            connectionManager.register(conn)
            connectionManager.emitEvent(ConnectionEvent.Connected(userId, conn))
            connectionManager.startReceiving(conn)
            connectionManager.startHeartbeat(conn)
        } catch (e: Exception) {
            Log.e(TAG, "处理客户端失败", e)
            socket.close()
        }
    }

    private fun performHandshake(reader: PacketReader, writer: PacketWriter): String? {
        return try {
            val packet = reader.read()
            if (packet.type != PacketType.HANDSHAKE) return null

            val hs = json.decodeFromString<ChatProtocol.Handshake>(
                String(packet.body, Charsets.UTF_8)
            )
            hs.e2ePublicKey?.let { peerKey ->
                val myKey = e2e.acceptHandshake(hs.senderId, peerKey)
                val ack = ChatProtocol.Handshake(senderId = hs.senderId, e2ePublicKeyAck = myKey)
                writer.write(
                    Packet(
                        PacketType.HANDSHAKE,
                        json.encodeToString<ChatProtocol>(ack).toByteArray(Charsets.UTF_8)
                    )
                )
            }
            hs.senderId
        } catch (_: Exception) {
            null
        }
    }
}