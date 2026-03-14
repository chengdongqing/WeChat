package top.chengdongqing.wechat.data.network.service.modules

import android.util.Log
import jakarta.inject.Inject
import jakarta.inject.Singleton
import top.chengdongqing.wechat.data.network.connection.bluetooth.ConnectionManager
import top.chengdongqing.wechat.data.network.connection.bluetooth.SocketServer
import top.chengdongqing.wechat.data.network.messaging.MessageReceiver
import top.chengdongqing.wechat.data.network.transfer.WifiLockManager

@Singleton
class BluetoothChatModule @Inject constructor(
    private val socketServer: SocketServer,
    private val connectionManager: ConnectionManager,
    private val wifiLockManager: WifiLockManager,
    private val messageReceiver: MessageReceiver,
) {
    companion object {
        private const val TAG = "BluetoothChatModule"
    }

    suspend fun start() {
        // 申请Wi-Fi锁，后台通信保活
        wifiLockManager.acquireKeepAlive()

        // 开始接受连接
        socketServer.start()
        // 开始接收消息
        messageReceiver.start()

        Log.d(TAG, "蓝牙聊天模块已启动")
    }

    fun stop() {
        socketServer.stop()
        connectionManager.closeAll()
        Log.d(TAG, "蓝牙聊天模块已停止")
    }
}