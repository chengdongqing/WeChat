package top.chengdongqing.wechat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import top.chengdongqing.wechat.core.di.DefaultScope
import top.chengdongqing.wechat.core.util.clearAllCache
import top.chengdongqing.wechat.features.settings.domain.repository.DisplaySettingsRepository

@HiltAndroidApp
class WeApplication : Application() {

    @Inject
    lateinit var displaySettingsRepository: DisplaySettingsRepository

    @Inject
    @DefaultScope
    lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()

        /**
         * 恢复主题、语言等设置
         */
        runBlocking {
            displaySettingsRepository.restoreOnStartup()
        }

        /**
         * 自动清理之前产生的缓存
         */
        scope.launch {
            clearAllCache()
        }
    }
}