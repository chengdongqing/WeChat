package top.chengdongqing.wechat.core.utils

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration

const val DefaultDateFormatter = "yyyy-MM-dd"
const val DefaultTimeFormatter = "HH:mm:ss"
const val DefaultDateTimeFormatter = "$DefaultDateFormatter $DefaultTimeFormatter"
const val ChineseDateFormatter = "yyyy年MM月dd日"
const val ChineseDateWeekFormatter = "$ChineseDateFormatter EEEE"

/**
 * 格式化时间
 *
 * @param milliseconds 毫秒数
 * @param pattern 格式
 */
fun formatTime(milliseconds: Long, pattern: String = DefaultDateTimeFormatter): String {
    return Instant.ofEpochMilli(milliseconds).atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern(pattern, Locale.CHINA))
}

/**
 * 格式化时长
 *
 * @param isFull 是否格式化为完整时长
 */
fun Duration.format(isFull: Boolean = false): String {
    val hours = inWholeSeconds / HOUR_IN_SECONDS
    val minutes = (inWholeSeconds % HOUR_IN_SECONDS) / MINUTE_IN_SECONDS
    val seconds = inWholeSeconds % MINUTE_IN_SECONDS

    return when {
        hours > 0 || isFull -> "%02d:%02d:%02d".format(hours, minutes, seconds)
        else -> "%02d:%02d".format(minutes, seconds)
        // %02d: 格式说明符，用于格式化整数。d 表示整数，02 表示如果数字少于两位，会在前面补零以达到两位数
    }
}

/**
 * 格式化聊天时间
 */
fun formatChatTime(timestamp: Long): String {
    val targetInstant = Instant.ofEpochMilli(timestamp)
    val target = LocalDateTime.ofInstant(targetInstant, ZoneId.systemDefault())
    val now = LocalDateTime.now()

    val targetDate = target.toLocalDate()
    val nowDate = now.toLocalDate()

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val monthDayFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
    val yearMonthDayFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")

    return when {
        // 同一天
        targetDate.isEqual(nowDate) -> {
            target.format(timeFormatter)
        }

        // 昨天
        targetDate.isEqual(nowDate.minusDays(1)) -> {
            "昨天 ${target.format(timeFormatter)}"
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
            "$weekDay ${target.format(timeFormatter)}"
        }

        // 今年以内
        target.year == now.year -> {
            target.format(monthDayFormatter)
        }

        // 往年
        else -> {
            target.format(yearMonthDayFormatter)
        }
    }
}

private const val HOUR_IN_SECONDS = 3600
private const val MINUTE_IN_SECONDS = 60