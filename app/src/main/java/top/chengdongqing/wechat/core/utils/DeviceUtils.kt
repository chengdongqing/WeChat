package top.chengdongqing.wechat.core.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import org.webrtc.Camera2Enumerator
import org.webrtc.Size

/**
 * 获取当前设备的名称
 */
fun Context.getDeviceName(): String {
    // 优先尝试获取全局设备名称（通常是用户在设置里改的名字）
    val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
        Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
    } else {
        null
    }

    // 如果拿不到，就用手机型号（比如 "Samsung S23"）
    return deviceName ?: "${Build.MANUFACTURER} ${Build.MODEL}"
}

/**
 * 获取摄像头最佳分辨率
 */
fun Context.getBestSupportedResolution(isFront: Boolean): Size {
    val enumerator = Camera2Enumerator(this)
    val deviceNames = enumerator.deviceNames
    val targetDevice = deviceNames.find {
        if (isFront) enumerator.isFrontFacing(it) else enumerator.isBackFacing(it)
    } ?: deviceNames.first()

    val formats = enumerator.getSupportedFormats(targetDevice) ?: return Size(1280, 720)

    // 过滤掉帧率低于 30 的，按宽度从大到小排序
    val bestFormat = formats
        .filter { it.framerate.max >= 30 }
        .maxByOrNull { it.width * it.height }

    return if (bestFormat != null) {
        Size(bestFormat.width, bestFormat.height)
    } else {
        Size(1280, 720) // 保底方案
    }
}