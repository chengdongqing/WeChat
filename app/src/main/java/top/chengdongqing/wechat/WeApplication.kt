package top.chengdongqing.wechat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.core.util.clearAllCache
import top.chengdongqing.wechat.data.network.transfer.TransferRecoveryManager

@HiltAndroidApp
class WeApplication : Application() {

    @Inject
    @IoScope
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var transferRecoveryManager: TransferRecoveryManager

    override fun onCreate() {
        super.onCreate()

        /**
         * 自动清理之前产生的缓存
         */
        scope.launch {
            clearAllCache()
        }

        /**
         * 恢复未完成的传输的状态
         * 清理过期的分片文件等
         */
        transferRecoveryManager.recover()
    }
}