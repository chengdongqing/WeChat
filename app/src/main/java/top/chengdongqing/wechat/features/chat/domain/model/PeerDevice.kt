package top.chengdongqing.wechat.features.chat.domain.model

/**
 * 通用对端设备模型（纯业务数据，不含 Android 框架对象）
 *
 * 蓝牙和 WiFi Direct 设备的统一抽象，供 domain/UI 层使用。
 * 与原生 Android 设备对象（BluetoothDevice、WifiP2pDevice）的映射
 * 由各自的 ViewModel 通过 id → native device 的 Map 维护。
 */
sealed class PeerDevice {
    abstract val id: String          // 唯一标识（MAC 地址）
    abstract val name: String        // 显示名称
    abstract val isPaired: Boolean   // 是否已配对/连接过
    abstract val signalStrength: Int // 信号强度（RSSI），WiFi Direct 无此值填 0

    data class Bluetooth(
        override val id: String,
        override val name: String,
        override val isPaired: Boolean,
        override val signalStrength: Int = 0
    ) : PeerDevice()

    data class WiFiDirect(
        override val id: String,
        override val name: String,
        override val isPaired: Boolean,
        override val signalStrength: Int = 0
    ) : PeerDevice()
}
