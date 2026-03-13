package top.chengdongqing.wechat.features.settings.domain.model

import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.network.connection.ConnectionMode

val ConnectionMode.labelRes: Int
    get() = when (this) {
        ConnectionMode.WiFiLan -> R.string.connection_wifi_lan
        ConnectionMode.WiFiDirect -> R.string.connection_wifi_direct
        ConnectionMode.Bluetooth -> R.string.connection_bluetooth
    }

val ConnectionMode.descriptionRes: Int
    get() = when (this) {
        ConnectionMode.WiFiLan -> R.string.connection_wifi_lan_desc
        ConnectionMode.WiFiDirect -> R.string.connection_wifi_direct_desc
        ConnectionMode.Bluetooth -> R.string.connection_bluetooth_desc
    }