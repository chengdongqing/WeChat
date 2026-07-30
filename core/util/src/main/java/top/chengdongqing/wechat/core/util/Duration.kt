package top.chengdongqing.wechat.core.util

import kotlin.time.Duration

private const val SecondsPerHour = 3_600
private const val SecondsPerMinute = 60

/**
 * Formats a duration as `mm:ss`, or `hh:mm:ss` when it contains hours or [includeHours] is true.
 */
fun Duration.format(includeHours: Boolean = false): String {
    val hours = inWholeSeconds / SecondsPerHour
    val minutes = (inWholeSeconds % SecondsPerHour) / SecondsPerMinute
    val seconds = inWholeSeconds % SecondsPerMinute

    return if (hours > 0 || includeHours) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
