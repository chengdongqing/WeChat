package top.chengdongqing.wechat.data.network.service.chat

import android.util.Log
import jakarta.inject.Inject
import jakarta.inject.Singleton
import top.chengdongqing.wechat.data.network.connection.ConnectionManager
import top.chengdongqing.wechat.data.network.connection.bluetooth.BtSocketServer
import top.chengdongqing.wechat.data.network.messaging.MessageReceiver
import top.chengdongqing.wechat.data.network.service.ServiceModule

/**
 * 蓝牙聊天模块
 */
@Singleton
class BtChatHandler @Inject constructor(
    private val socketServer: BtSocketServer,
    private val connectionManager: ConnectionManager,
    private val messageReceiver: MessageReceiver,
) : ServiceModule {
    private companion object {
        private const val TAG = "BtChatHandler"
    }

    override fun start() {
        runCatching {
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
        }.onSuccess {
            Log.d(TAG, "蓝牙聊天模块已停止")
        }
    }
}