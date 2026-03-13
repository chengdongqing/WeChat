package top.chengdongqing.wechat.data.network.service.modules

import android.util.Log
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.network.connection.ConnectionMode
import top.chengdongqing.wechat.data.network.connection.bluetooth.BtConnectionManager
import top.chengdongqing.wechat.data.network.connection.bluetooth.RfcommClient
import top.chengdongqing.wechat.data.network.connection.bluetooth.RfcommServer

@Singleton
class BluetoothChatModule @Inject constructor(
    private val rfcommServer: RfcommServer,
    private val rfcommClient: RfcommClient,
    private val btConnectionManager: BtConnectionManager,
    private val connectionInfoDao: ConnectionInfoDao,
) {
    companion object {
        private const val TAG = "BluetoothChatModule"
    }

    suspend fun start(userId: String, scope: CoroutineScope) {
        rfcommServer.start()
        connectToFriends(userId, scope)
        Log.d(TAG, "蓝牙聊天模块已启动")
    }

    fun stop() {
        rfcommServer.stop()
        btConnectionManager.closeAll()
        Log.d(TAG, "蓝牙聊天模块已停止")
    }

    /**
     * 从 connection_info 表取出所有蓝牙好友，逐一发起 RFCOMM 连接
     * 对应 WiFiLanChatModule 里 NSD 发现设备后的 SocketClient.connect()
     */
    private fun connectToFriends(myUserId: String, scope: CoroutineScope) {
        scope.launch {
            connectionInfoDao.getByMode(ConnectionMode.Bluetooth)
                .filter { it.bluetoothAddress != null }
                .forEach { info ->
                    if (btConnectionManager.isConnected(info.userId)) return@forEach

                    rfcommClient.connect(
                        userId = info.userId,
                        macAddress = info.bluetoothAddress!!,
                        myUserId = myUserId
                    ).onSuccess {
                        Log.d(TAG, "RFCOMM 已连接: ${info.userId}")
                    }
                }
        }
    }
}