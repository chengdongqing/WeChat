package top.chengdongqing.wechat.data.network.service.chat

import android.util.Log
import jakarta.inject.Inject
import jakarta.inject.Singleton
import top.chengdongqing.wechat.data.network.connection.bluetooth.BtConnectionManager
import top.chengdongqing.wechat.data.network.connection.bluetooth.BtSocketServer
import top.chengdongqing.wechat.data.network.messaging.MessageReceiver
import top.chengdongqing.wechat.data.network.service.ServiceModule
import top.chengdongqing.wechat.data.network.transfer.WifiLockManager

/**
 * 蓝牙聊天模块
 */
@Singleton
class BluetoothChatModule @Inject constructor(
    private val socketServer: BtSocketServer,
    private val connectionManager: BtConnectionManager,
    private val wifiLockManager: WifiLockManager,
    private val messageReceiver: MessageReceiver,
) : ServiceModule {
    private companion object {
        private const val TAG = "BluetoothChatModule"
    }

    override fun start() {
        runCatching {
            // 申请Wi-Fi锁，后台通信保活
            wifiLockManager.acquireKeepAlive()
            // 开始接受连接
            socketServer.start()
            // 开始接收消息
            messageReceiver.start()
        }.onSuccess {
            Log.d(TAG, "蓝牙聊天模块已启动")
        }.onFailure {
            Log.e(TAG, "蓝牙聊天模块启动失败", it)
        }
    }

    override fun stop() {
        runCatching {
            // 关闭所有连接
            connectionManager.closeAll()
            // 关闭socket服务
            socketServer.stop()
            // 释放Wi-Fi 锁
            wifiLockManager.releaseKeepAlive()
        }.onSuccess {
            Log.d(TAG, "蓝牙聊天模块已停止")
        }
    }
}