package top.chengdongqing.wechat.core.util

import android.content.Context
import androidx.core.content.edit

class IdManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun getMyId(): String {
        var id = prefs.getString("device_uuid", null)
        if (id == null) {
            id = randomUUID()
            prefs.edit { putString("device_uuid", id) }
        }
        return id
    }
}
