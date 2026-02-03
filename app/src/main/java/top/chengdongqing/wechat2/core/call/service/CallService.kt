package top.chengdongqing.wechat2.core.call.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class CallService : Service() {
    // Foreground Service 保持通话活跃
    // 处理来电通知
    // 管理通话生命周期
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}