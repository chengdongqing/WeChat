package top.chengdongqing.wechat

import android.app.Application
import top.chengdongqing.wechat.core.media.SoundTipPlayer
import top.chengdongqing.wechat.core.utils.clearAllCache

class WechatApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 清理之前产生的缓存
        clearAllCache()

        // 初始化提示音播放器
        SoundTipPlayer.init(this)
    }
}