package top.chengdongqing.wechat.data.model

import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager

/**
 * 连接能力定义
 */
object ConnectionCapabilities {
    const val WIFI_LAN = 1 shl 0        // 0x01
    const val WIFI_DIRECT = 1 shl 1     // 0x02
    const val BLUETOOTH = 1 shl 2       // 0x04
    const val NFC = 1 shl 3             // 0x08
    const val SOUND_WAVE = 1 shl 4      // 0x10
    const val UWB = 1 shl 5             // 0x20

    /**
     * 检查是否支持某个能力
     */
    fun hasCapability(capabilities: Int, capability: Int): Boolean {
        return (capabilities and capability) != 0
    }

    /**
     * 获取当前设备的能力
     */
    fun getDeviceCapabilities(context: Context): Int {
        var caps = 0

        // WiFi LAN
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager.isWifiEnabled) {
            caps = caps or WIFI_LAN
        }

        // WiFi Direct
        val wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        if (wifiP2pManager != null) {
            caps = caps or WIFI_DIRECT
        }

        // Bluetooth
        val bluetoothAdapter =
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
            caps = caps or BLUETOOTH
        }

        return caps
    }
}