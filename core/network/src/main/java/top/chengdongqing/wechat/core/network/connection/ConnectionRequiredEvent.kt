package top.chengdongqing.wechat.core.network.connection

/**
 * 需要用户手动选择设备才能建立连接的事件
 * 蓝牙和 WiFi Direct 模式下，没有已知连接信息时触发
 */
sealed class ConnectionRequiredEvent {
    data object Bluetooth : ConnectionRequiredEvent()
    data object WiFiDirect : ConnectionRequiredEvent()
}