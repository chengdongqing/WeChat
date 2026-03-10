package top.chengdongqing.wechat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.core.util.clearAllCache

@HiltAndroidApp
class WeApplication : Application() {

    @Inject
    @IoScope
    lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()

        /**
         * 自动清理之前产生的缓存
         */
        scope.launch {
            clearAllCache()
        }
    }
}