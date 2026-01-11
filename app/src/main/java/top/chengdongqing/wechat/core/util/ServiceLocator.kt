package top.chengdongqing.wechat.core.util

import android.content.Context
import top.chengdongqing.wechat.core.protocol.MessageDispatcher
import top.chengdongqing.wechat.data.local.DatabaseModule
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

    private var dispatcher: MessageDispatcher? = null

    fun getMessageDispatcher(context: Context): MessageDispatcher {
        return dispatcher ?: synchronized(this) {
            // 这里初始化时传入 Context 和 Room 数据库的 DAO
            val db = DatabaseModule.getDatabase(context)
            MessageDispatcher(context, db.messageDao()).also { dispatcher = it }
        }
    }
}