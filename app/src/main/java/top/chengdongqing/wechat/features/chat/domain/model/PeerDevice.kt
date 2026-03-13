package top.chengdongqing.wechat.features.chat.domain.model

import android.bluetooth.BluetoothDevice
import android.net.wifi.p2p.WifiP2pDevice

/**
 * 通用对端设备模型
 * 蓝牙和 WiFi Direct 设备的统一抽象
 */
sealed class PeerDevice {
    abstract val id: String          // 唯一标识（MAC 地址）
    abstract val name: String        // 显示名称
    abstract val isPaired: Boolean   // 是否已配对/连接过
    abstract val signalStrength: Int // 信号强度（RSSI），WiFi Direct 无此值填0

    data class Bluetooth(
        override val id: String,
        override val name: String,
        override val isPaired: Boolean,
        override val signalStrength: Int = 0,
        val device: BluetoothDevice
    ) : PeerDevice()

    data class WiFiDirect(
        override val id: String,
        override val name: String,
        override val isPaired: Boolean,
        override val signalStrength: Int = 0,
        val device: WifiP2pDevice
    ) : PeerDevice()
}

enum class WiFiDirectRole { None, Owner, Client }

data class PeerDeviceUiState(
    val isScanning: Boolean = false,
    val pairedDevices: List<PeerDevice> = emptyList(),
    val nearbyDevices: List<PeerDevice> = emptyList(),
    val connectingDeviceId: String? = null,
    val role: WiFiDirectRole = WiFiDirectRole.None,
    val error: String? = null
)