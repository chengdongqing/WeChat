package top.chengdongqing.wechat.core.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatTime(timestamp: Long): String {
    // 1. 将时间戳转为本地日期时间对象
    val msgTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault()
    )
    val now = LocalDateTime.now()

    // 2. 准备格式化器
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val monthDayFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    val yearFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    return when {
        // 同一天：只显时间 (14:20)
        msgTime.toLocalDate().isEqual(now.toLocalDate()) -> {
            msgTime.format(timeFormatter)
        }

        // 昨天：显示 昨天 14:20
        msgTime.toLocalDate().isEqual(now.toLocalDate().minusDays(1)) -> {
            "昨天 ${msgTime.format(timeFormatter)}"
        }

        // 同一年：显示 05-12 14:20
        msgTime.year == now.year -> {
            msgTime.format(monthDayFormatter)
        }

        // 跨年了：显示 2024-12-01
        else -> {
            msgTime.format(yearFormatter)
        }
    }
}