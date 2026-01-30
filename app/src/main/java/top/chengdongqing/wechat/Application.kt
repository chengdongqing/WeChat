package top.chengdongqing.wechat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import top.chengdongqing.wechat.core.media.SoundTipPlayer
import top.chengdongqing.wechat.core.utils.clearAllCache

@HiltAndroidApp
class WechatApplication : Application() {
    @Inject
    lateinit var soundPlayer: SoundTipPlayer // 初始化提示音播放器

    override fun onCreate() {
        super.onCreate()

        // 清理之前产生的缓存
        clearAllCache()
    }
}