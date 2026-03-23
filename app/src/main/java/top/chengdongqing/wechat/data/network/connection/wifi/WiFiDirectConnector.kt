package top.chengdongqing.wechat.data.network.connection.wifi

import android.annotation.SuppressLint
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class WiFiDirectConnector @Inject constructor() {
    companion object {
        private const val TAG = "WiFiDirectConnector"
    }

    /**
     * 发起 P2P 连接
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(
        p2pManager: WifiP2pManager,
        channel: WifiP2pManager.Channel,
        device: WifiP2pDevice
    ) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
            groupOwnerIntent = 0
        }

        // 发起 P2P 连接（系统弹窗让用户确认）
        suspendCancellableCoroutine { cont ->
            p2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    if (cont.isActive) {
                        cont.resume(Unit)
                    }
                }

                override fun onFailure(reason: Int) {
                    Log.w(TAG, "P2P 连接请求失败: $reason")
                    if (cont.isActive) {
                        cont.resumeWithException(Exception("P2P 连接失败: $reason"))
                    }
                }
            })
        }
    }
}