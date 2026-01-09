package top.chengdongqing.wechat.data.model

enum class P2pMode(val label: String) {
    WIFI_LAN("局域网模式"),      // 基于同 Wi-Fi 下的 UDP 广播
    WIFI_DIRECT("Wi-Fi 直连"), // 基于 WifiP2pManager (无需路由器)
    BLUETOOTH("蓝牙模式")      // 基于经典蓝牙 RFCOMM
}