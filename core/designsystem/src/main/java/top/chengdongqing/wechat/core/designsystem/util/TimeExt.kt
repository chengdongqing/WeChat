package top.chengdongqing.wechat.core.designsystem.util

import kotlin.time.Duration

private const val HourInSeconds = 3600
private const val MinuteInSeconds = 60

/**
 * 格式化时长
 *
 * @param isFull 是否格式化为完整时长
 */
fun Duration.format(isFull: Boolean = false): String {
    val hours = inWholeSeconds / HourInSeconds
    val minutes = (inWholeSeconds % HourInSeconds) / MinuteInSeconds
    val seconds = inWholeSeconds % MinuteInSeconds

    return when {
        hours > 0 || isFull -> "%02d:%02d:%02d".format(hours, minutes, seconds)
        else -> "%02d:%02d".format(minutes, seconds)
    }
}
