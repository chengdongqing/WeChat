package top.chengdongqing.wechat.core.common.time

import android.content.res.Resources
import top.chengdongqing.wechat.core.model.AppLanguage
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import top.chengdongqing.wechat.core.designsystem.R as DesignR

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val MonthDayFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
private val YearMonthDayFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")
private val YearMonthFormatterZh = DateTimeFormatter.ofPattern("yyyy年MM月")
private val YearMonthFormatterEn = DateTimeFormatter.ofPattern("yyyy-MM")
private val FullDateTimeFormatterZh =
    DateTimeFormatter.ofPattern("yyyy年M月d日 EEE HH:mm", Locale.CHINESE)
private val FullDateTimeFormatterEn =
    DateTimeFormatter.ofPattern("EEE, M/d/yy HH:mm", Locale.ENGLISH)

/**
 * 将时间戳显示为相对时间
 *
 * 例：
 * 当天：14:23
 * 昨天：昨天 14:23
 * 一周内：星期二 14:23
 * 今年：8月6日 14:23
 * 往年：2026年8月6日 14:23
 */
fun Long.toRelativeDateTime(resources: Resources): String {
    val target = this.asLocalDateTime()
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
            "${resources.getString(DesignR.string.time_yesterday)} ${target.format(TimeFormatter)}"
        }

        // 一周内 (2-7天前)
        targetDate.isAfter(nowDate.minusDays(7)) -> {
            val weekDayRes = when (target.dayOfWeek) {
                DayOfWeek.MONDAY -> DesignR.string.time_monday
                DayOfWeek.TUESDAY -> DesignR.string.time_tuesday
                DayOfWeek.WEDNESDAY -> DesignR.string.time_wednesday
                DayOfWeek.THURSDAY -> DesignR.string.time_thursday
                DayOfWeek.FRIDAY -> DesignR.string.time_friday
                DayOfWeek.SATURDAY -> DesignR.string.time_saturday
                DayOfWeek.SUNDAY -> DesignR.string.time_sunday
                else -> DesignR.string.time_monday
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
 * 将时间戳格式化为年月的形式
 *
 * 例：
 * 中文：2026年08月
 * 英文：2026-08
 */
fun Long.toYearMonthDate(language: AppLanguage): String {
    val target = this.asLocalDateTime().toLocalDate()

    return when (language) {
        AppLanguage.Chinese -> target.format(YearMonthFormatterZh)
        else -> target.format(YearMonthFormatterEn)
    }
}

/**
 * 将时间戳格式化为完整的日期时间形式
 *
 * 例：
 * 今年：8月6日 周四 14:23 / Thu, 8/6 14:23
 * 往年：2025年8月6日 周四 14:23 / Thu, 8/6/25 14:23
 */
fun Long.toFullDateTime(language: AppLanguage): String {
    val target = this.asLocalDateTime()

    return when (language) {
        AppLanguage.Chinese -> {
            val formatter = FullDateTimeFormatterZh
            target.format(formatter)
        }

        else -> {
            val formatter = FullDateTimeFormatterEn
            target.format(formatter)
        }
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

private fun Long.asLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())