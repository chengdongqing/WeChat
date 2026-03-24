package top.chengdongqing.wechat.core.common.util

import android.content.res.Resources
import top.chengdongqing.wechat.core.common.R
import top.chengdongqing.wechat.core.model.AppLanguage
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration

private const val HourInSeconds = 3600
private const val MinuteInSeconds = 60

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val MonthDayFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
private val YearMonthDayFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")
private val YearMonthFormatterZh = DateTimeFormatter.ofPattern("yyyy年MM月")
private val YearMonthFormatterEn = DateTimeFormatter.ofPattern("yyyy-MM")

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
fun Long.toChatDisplayTime(resources: Resources): String {
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
            "${resources.getString(R.string.time_yesterday)} ${target.format(TimeFormatter)}"
        }

        // 一周内 (2-7天前)
        targetDate.isAfter(nowDate.minusDays(7)) -> {
            val weekDayRes = when (target.dayOfWeek) {
                DayOfWeek.MONDAY -> R.string.time_monday
                DayOfWeek.TUESDAY -> R.string.time_tuesday
                DayOfWeek.WEDNESDAY -> R.string.time_wednesday
                DayOfWeek.THURSDAY -> R.string.time_thursday
                DayOfWeek.FRIDAY -> R.string.time_friday
                DayOfWeek.SATURDAY -> R.string.time_saturday
                DayOfWeek.SUNDAY -> R.string.time_sunday
                else -> R.string.time_monday
            }
            "${resources.getString(weekDayRes)} ${target.format(TimeFormatter)}"
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

/**
 * 格式化为年月
 */
fun Long.toYearMonthDisplay(language: AppLanguage): String {
    val targetInstant = Instant.ofEpochMilli(this)
    val target = LocalDate.ofInstant(targetInstant, ZoneId.systemDefault())

    return when (language) {
        AppLanguage.Chinese -> target.format(YearMonthFormatterZh)
        else -> target.format(YearMonthFormatterEn)
    }
}

/**
 * 判断该时间戳是否在当前时间的前 [seconds] 秒内
 */
fun Long.isWithinSeconds(seconds: Int = 5 * 60): Boolean {
    val diff = System.currentTimeMillis() - this
    val threshold = seconds * 1000L
    return diff in 0..threshold
}