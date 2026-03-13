package top.chengdongqing.wechat.data.network.connection

import top.chengdongqing.wechat.data.network.protocol.Packet

/**
 * 需要用户手动选择设备才能建立连接的事件
 * 蓝牙和 WiFi Direct 模式下，没有已知连接信息时触发
 */
sealed class ConnectionRequiredEvent {
    abstract val userId: String
    abstract val pendingPacket: Packet

    data class Bluetooth(
        override val userId: String,
        override val pendingPacket: Packet
    ) : ConnectionRequiredEvent()

    data class WiFiDirect(
        override val userId: String,
        override val pendingPacket: Packet
    ) : ConnectionRequiredEvent()
}