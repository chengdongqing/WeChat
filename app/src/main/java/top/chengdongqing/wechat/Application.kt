package top.chengdongqing.wechat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import top.chengdongqing.wechat.core.util.clearAllCache

@HiltAndroidApp
class WeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        /**
         * 自动清理之前产生的缓存
         */
        clearAllCache()
    }
}