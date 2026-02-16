package top.chengdongqing.wechat

import android.app.Application
import android.content.Intent
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import top.chengdongqing.wechat.data.network.service.P2PService

@HiltAndroidApp
class WeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

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