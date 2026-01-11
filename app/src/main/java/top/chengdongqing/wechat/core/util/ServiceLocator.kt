package top.chengdongqing.wechat.core.util

import android.content.Context
import top.chengdongqing.wechat.data.network.WifiLanManager

object ServiceLocator {
    private var wifiLanManager: WifiLanManager? = null

    fun getWifiLanManager(context: Context): WifiLanManager {
        return wifiLanManager ?: synchronized(this) {
            wifiLanManager ?: WifiLanManager(context.applicationContext).also {
                wifiLanManager = it
            }
        }
    }
}