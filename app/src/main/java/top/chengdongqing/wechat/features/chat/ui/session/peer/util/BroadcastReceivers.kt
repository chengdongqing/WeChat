package top.chengdongqing.wechat.features.chat.ui.session.peer.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import top.chengdongqing.wechat.features.chat.ui.session.peer.BluetoothPeerViewModel
import top.chengdongqing.wechat.features.chat.ui.session.peer.WiFiDirectPeerViewModel

/**
 * 蓝牙扫描广播接收器
 * 触发设备可见性请求，并在找到设备或扫描结束时回调 ViewModel
 */
fun buildBluetoothReceiver(
    viewModel: BluetoothPeerViewModel,
    onStartDiscoverable: () -> Unit,
): BroadcastReceiver {
    onStartDiscoverable()
    return object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // 设备详情
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    } ?: return
                    // 信号强度
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0).toInt()

                    // 触发回调
                    viewModel.onClassicDeviceFound(device, rssi)
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> viewModel.onDiscoveryFinished()
            }
        }
    }
}

/**
 * Wi-Fi Direct 设备变化广播接收器
 * 收到对等列表变更时，主动请求最新设备列表并通知 ViewModel
 */
@SuppressLint("MissingPermission")
fun buildWifiDirectReceiver(
    context: Context,
    viewModel: WiFiDirectPeerViewModel,
): BroadcastReceiver {
    return object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) return
            val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
            val channel = manager.initialize(context, ctx.mainLooper, null)
            manager.requestPeers(channel) { peers: WifiP2pDeviceList ->
                viewModel.onPeersChanged(peers.deviceList.toList())
            }
        }
    }
}