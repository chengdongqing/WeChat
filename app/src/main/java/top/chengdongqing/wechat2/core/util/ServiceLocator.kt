package top.chengdongqing.wechat2.core.util

import android.content.Context
import top.chengdongqing.wechat2.core.protocol.MessageDispatcher
import top.chengdongqing.wechat2.core.protocol.MessageDispatcherImpl
import top.chengdongqing.wechat2.data.local_1.DatabaseModule
import top.chengdongqing.wechat2.data.network.WifiLanManager

object ServiceLocator {
    private var wifiLanManager: WifiLanManager? = null

    fun getWifiLanManager(context: Context): WifiLanManager {
        return wifiLanManager ?: synchronized(this) {
            wifiLanManager ?: WifiLanManager(context.applicationContext).also {
                wifiLanManager = it
            }
        }
    }

    private var dispatcher: MessageDispatcher? = null

    fun getMessageDispatcher(context: Context): MessageDispatcher {
        return dispatcher ?: synchronized(this) {
            val db = DatabaseModule.getDatabase(context)
            MessageDispatcherImpl(db.messageDao()).also { dispatcher = it }
        }
    }
}