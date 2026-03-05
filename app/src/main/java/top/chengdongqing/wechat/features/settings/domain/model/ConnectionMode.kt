package top.chengdongqing.wechat.features.settings.domain.model

/**
 * 连接模式
 */
enum class ConnectionMode(
    val label: String,
    val description: String
) {
    WifiLan(
        label = "Wi-Fi 局域网",
        description = "在同一 Wi-Fi 下高速传输，适合大文件发送与稳定通话。"
    ),
    WifiDirect(
        label = "Wi-Fi 直连 (P2P)",
        description = "无需接入路由器，设备间直接互联，适合户外无网络环境。"
    ),
    Bluetooth(
        label = "蓝牙",
        description = "低功耗发现周边设备，适合近距离发送短消息。"
    );
}