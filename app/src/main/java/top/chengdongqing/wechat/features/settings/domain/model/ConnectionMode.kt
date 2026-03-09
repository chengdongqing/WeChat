package top.chengdongqing.wechat.features.settings.domain.model

import androidx.annotation.StringRes
import top.chengdongqing.wechat.R

/**
 * 连接模式
 */
enum class ConnectionMode(
    @get:StringRes val labelRes: Int,
    @get:StringRes val descriptionRes: Int
) {
    WifiLan(R.string.connection_wifi_lan, R.string.connection_wifi_lan_desc),
    WifiDirect(R.string.connection_wifi_direct, R.string.connection_wifi_direct_desc),
    Bluetooth(R.string.connection_bluetooth, R.string.connection_bluetooth_desc);
}