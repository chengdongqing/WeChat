package top.chengdongqing.wechat.core.util

import android.content.Context
import androidx.core.content.edit

/**
 * 设备ID管理器
 */
class IdManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    /**
     * 获取设备自定义的唯一ID
     */
    fun getDeviceId(): String {
        synchronized(this) {
            var id = prefs.getString("device_uuid", null)
            if (id == null) {
                id = randomUUID()
                prefs.edit { putString("device_uuid", id) }
            }
            return id
        }
    }
}