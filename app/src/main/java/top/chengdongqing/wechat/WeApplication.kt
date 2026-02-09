package top.chengdongqing.wechat

import android.app.Application
import android.content.Intent
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import top.chengdongqing.wechat.core.media.SoundTipPlayer
import top.chengdongqing.wechat.core.util.clearAllCache
import top.chengdongqing.wechat.data.network.service.P2PService

@HiltAndroidApp
class WeApplication : Application() {
    @Inject
    lateinit var soundPlayer: SoundTipPlayer // 初始化提示音播放器

    override fun onCreate() {
        super.onCreate()

        // 清理之前产生的缓存
        clearAllCache()

        // 启动P2P服务
        Intent(this, P2PService::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
}