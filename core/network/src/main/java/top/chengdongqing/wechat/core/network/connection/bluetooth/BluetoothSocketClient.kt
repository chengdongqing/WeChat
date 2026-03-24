package top.chengdongqing.wechat.core.network.connection.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.network.config.TransferConfig
import top.chengdongqing.wechat.core.network.connection.ConnectionEvent
import top.chengdongqing.wechat.core.network.connection.ConnectionManager
import top.chengdongqing.wechat.core.network.connection.PeerConnection
import top.chengdongqing.wechat.core.network.connection.PeerHandshakeHandler
import top.chengdongqing.wechat.core.network.model.PacketReader
import top.chengdongqing.wechat.core.network.model.PacketWriter

/**
 * RFCOMM 客户端，负责主动发起到对方设备的蓝牙连接。
 */
@Singleton
class BluetoothSocketClient @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val handshakeHandler: PeerHandshakeHandler,
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BtSocketClient"
    }

    /**
     * 向 [macAddress] 对应的设备发起 RFCOMM 连接，成功后注册连接并启动收发。
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(
        userId: String,
        macAddress: String,
        myUserId: String,
    ): Result<PeerConnection> = withContext(Dispatchers.IO) {
        runCatching {
            val socket = createSocket(macAddress)

            val conn = PeerConnection(
                userId = userId,
                reader = PacketReader(socket.inputStream),
                writer = PacketWriter(socket.outputStream),
                maxConcurrentTransfers = Semaphore(TransferConfig.CONCURRENT_TRANSFERS_BT),
                isActiveProvider = { socket.isConnected },
                closeAction = { socket.close() },
            )
            // 保存连接
            connectionManager.register(conn)
            // 推送连接成功事件
            connectionManager.emitEvent(ConnectionEvent.Connected(userId, conn))

            // 发出握手包（携带己方公钥）
            conn.writer.write(handshakeHandler.buildHandshakePacket(userId, myUserId))

            // 开始接收数据
            connectionManager.startReceiving(conn) { packet ->
                handshakeHandler.handleHandshakeReply(conn, packet)
            }
            // 开始维持心跳
            connectionManager.startHeartbeat(conn)

            conn
        }.onFailure {
            Log.e(TAG, "连接失败: $userId", it)
            connectionManager.emitEvent(ConnectionEvent.Disconnected(userId, it.message))
        }
    }

    /**
     * 通过 MAC 地址获取远端设备并建立 RFCOMM Socket
     */
    private fun createSocket(macAddress: String): BluetoothSocket {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE)
                as BluetoothManager).adapter

        return adapter.getRemoteDevice(macAddress)
            .createRfcommSocketToServiceRecord(BluetoothSocketServer.RFCOMM_UUID)
            .apply {
                connect()
            }
    }
}