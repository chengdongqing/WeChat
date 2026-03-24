package top.chengdongqing.wechat.core.data.model

enum class ConnectionMode {
    WiFiLan,
    WiFiDirect,
    Bluetooth;

    companion object {
        fun fromName(name: String?) = entries.find { it.name == name } ?: WiFiLan
    }
}
