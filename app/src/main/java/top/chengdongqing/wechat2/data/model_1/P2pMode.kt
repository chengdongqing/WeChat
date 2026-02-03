package top.chengdongqing.wechat2.data.model_1

enum class P2pMode(val label: String) {
    WifiLan("局域网模式"),      // 基于同 Wi-Fi 下的 UDP 广播
    WifiDirect("Wi-Fi 直连"), // 基于 WifiP2pManager (无需路由器)
    Bluetooth("蓝牙模式")      // 基于经典蓝牙 RFCOMM
}