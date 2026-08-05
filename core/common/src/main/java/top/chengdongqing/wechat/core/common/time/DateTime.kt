package top.chengdongqing.wechat.core.common.time

import android.content.res.Resources
import top.chengdongqing.wechat.core.model.AppLanguage
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import top.chengdongqing.wechat.core.designsystem.R as DesignR

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val MonthDayFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
private val YearMonthDayFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")
private val YearMonthFormatterZh = DateTimeFormatter.ofPattern("yyyy年MM月")
private val YearMonthFormatterEn = DateTimeFormatter.ofPattern("yyyy-MM")

/**
 * 将时间戳显示为相对时间
 */
fun Long.toRelativeDateTime(resources: Resources): String {
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
 */
fun Long.toYearMonthDisplay(language: AppLanguage): String {
    val targetInstant = Instant.ofEpochMilli(this)
    val target = targetInstant.atZone(ZoneId.systemDefault()).toLocalDate()

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
