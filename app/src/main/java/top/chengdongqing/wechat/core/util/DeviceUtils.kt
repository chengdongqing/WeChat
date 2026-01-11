package top.chengdongqing.wechat.core.util

import android.content.Context
import android.os.Build
import android.provider.Settings

fun Context.getDeviceName(): String {
    // 1. 优先尝试获取全局设备名称（通常是用户在设置里改的名字）
    val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
        Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
    } else {
        null
    }

    // 2. 如果拿不到，就用手机型号（比如 "Samsung S23"）
    return deviceName ?: "${Build.MANUFACTURER} ${Build.MODEL}"
}