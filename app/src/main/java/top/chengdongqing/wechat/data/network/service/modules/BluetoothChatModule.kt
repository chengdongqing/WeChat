package top.chengdongqing.wechat.data.network.service.modules

import android.util.Log
import jakarta.inject.Inject
import jakarta.inject.Singleton
import top.chengdongqing.wechat.data.network.connection.bluetooth.BtConnectionManager
import top.chengdongqing.wechat.data.network.connection.bluetooth.BtSocketServer
import top.chengdongqing.wechat.data.network.messaging.MessageReceiver
import top.chengdongqing.wechat.data.network.transfer.WifiLockManager

@Singleton
class BluetoothChatModule @Inject constructor(
    private val socketServer: BtSocketServer,
    private val connectionManager: BtConnectionManager,
    private val wifiLockManager: WifiLockManager,
    private val messageReceiver: MessageReceiver,
) {
    private companion object {
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
        // 关闭所有连接
        connectionManager.closeAll()
        // 关闭socket服务
        socketServer.stop()
        // 释放Wi-Fi 锁
        wifiLockManager.releaseKeepAlive()

        Log.d(TAG, "蓝牙聊天模块已停止")
    }
}