package top.chengdongqing.wechat.core.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration

private const val HourInSeconds = 3600
private const val MinuteInSeconds = 60

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val MonthDayFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
private val YearMonthDayFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")

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
        // %02d: 格式说明符，用于格式化整数。d 表示整数，02 表示如果数字少于两位，会在前面补零以达到两位数
    }
}

/**
 * 格式化聊天时间
 */
fun Long.toChatDisplayTime(): String {
    val targetInstant = Instant.ofEpochMilli(this)
    val target = LocalDateTime.ofInstant(targetInstant, ZoneId.systemDefault())
    val now = LocalDateTime.now()
    val targetDate = target.toLocalDate()
    val nowDate = now.toLocalDate()

    return when {
        // 同一天
        targetDate.isEqual(nowDate) -> {
            target.format(TimeFormatter)
        }

        // 昨天
        targetDate.isEqual(nowDate.minusDays(1)) -> {
            "昨天 ${target.format(TimeFormatter)}"
        }

        // 一周内 (2-7天前)
        targetDate.isAfter(nowDate.minusDays(7)) -> {
            val weekDay = when (target.dayOfWeek) {
                DayOfWeek.MONDAY -> "周一"
                DayOfWeek.TUESDAY -> "周二"
                DayOfWeek.WEDNESDAY -> "周三"
                DayOfWeek.THURSDAY -> "周四"
                DayOfWeek.FRIDAY -> "周五"
                DayOfWeek.SATURDAY -> "周六"
                DayOfWeek.SUNDAY -> "周日"
                else -> ""
            }
            "$weekDay ${target.format(TimeFormatter)}"
        }

        // 今年以内
        target.year == now.year -> {
            target.format(MonthDayFormatter)
        }

        // 往年
        else -> {
            target.format(YearMonthDayFormatter)
        }
    }
}