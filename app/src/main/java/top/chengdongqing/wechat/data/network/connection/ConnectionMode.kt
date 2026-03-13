package top.chengdongqing.wechat.data.network.connection

/**
 * 连接模式
 */
enum class ConnectionMode {
    WiFiLan,
    WiFiDirect,
    Bluetooth;

    companion object {
        fun fromName(name: String?) =
            entries.find { it.name == name } ?: WiFiLan
    }
}