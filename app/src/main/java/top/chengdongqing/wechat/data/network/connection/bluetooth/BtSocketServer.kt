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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.network.config.TransferConfig
import top.chengdongqing.wechat.data.network.connection.ConnectionEvent
import top.chengdongqing.wechat.data.network.connection.PeerConnection
import top.chengdongqing.wechat.data.network.connection.PeerHandshakeHandler
import top.chengdongqing.wechat.data.network.model.PacketReader
import top.chengdongqing.wechat.data.network.model.PacketWriter
import java.util.UUID

/**
 * RFCOMM 服务端，负责监听并接入对方设备发起的蓝牙连接。
 */
@Singleton
class BtSocketServer @Inject constructor(
    private val connectionManager: BtConnectionManager,
    private val handshakeHandler: PeerHandshakeHandler,
    private val btBondManager: BtBondManager,
    @param:ApplicationContext private val context: Context,
    @param:IoScope private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "BtSocketServer"

        /** 固定 UUID，客户端必须用同一个 UUID 才能找到并连接此服务 */
        val RFCOMM_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        private const val SERVICE_NAME = "WeChat_Chat"
    }

    private val adapter by lazy { (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter }

    private var serverSocket: BluetoothServerSocket? = null

    /**
     * 创建 RFCOMM 服务 socket 并在后台启动 accept 循环
     */
    @SuppressLint("MissingPermission")
    fun start() {
        runCatching {
            serverSocket = adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, RFCOMM_UUID)
            scope.launch { acceptLoop() }
        }.onFailure {
            Log.e(TAG, "RFCOMM 服务启动失败", it)
        }
    }

    /**
     * 关闭服务 socket
     */
    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    /**
     * 持续等待客户端连接
     */
    private fun acceptLoop() {
        val socket = serverSocket ?: return
        while (true) {
            runCatching {
                val clientSocket = socket.accept()
                scope.launch { handleClient(clientSocket) }
            }.onFailure {
                Log.e(TAG, "接受连接异常，服务已停止")
                break
            }
        }
    }

    /**
     * 处理单条入站连接：握手 → 注册连接 → 启动收包和心跳。
     * 任意步骤失败都关闭 socket，不影响其他连接。
     */
    private suspend fun handleClient(socket: BluetoothSocket) = withContext(Dispatchers.IO) {
        try {
            val reader = PacketReader(socket.inputStream)
            val writer = PacketWriter(socket.outputStream)

            // 执行握手
            val userId = handshakeHandler.acceptHandshake(reader, writer) ?: run {
                Log.w(TAG, "握手失败，关闭连接")
                socket.close()
                return@withContext
            }

            val conn = PeerConnection(
                userId = userId,
                reader = reader,
                writer = writer,
                isActiveProvider = { socket.isConnected },
                maxConcurrentTransfers = Semaphore(TransferConfig.CONCURRENT_TRANSFERS_BT),
                closeAction = { socket.close() },
            )
            // 保存连接
            connectionManager.register(conn)
            // 推送连接事件
            connectionManager.emitEvent(ConnectionEvent.Connected(userId, conn))

            // 开始接收数据
            connectionManager.startReceiving(conn)
            // 开始维持心跳
            connectionManager.startHeartbeat(conn)

            // 保存连接信息
            btBondManager.saveToDB(userId, socket.remoteDevice)
        } catch (e: Exception) {
            Log.e(TAG, "处理客户端失败", e)
            socket.close()
        }
    }
}