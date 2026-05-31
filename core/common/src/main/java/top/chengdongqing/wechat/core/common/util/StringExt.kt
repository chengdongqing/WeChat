package top.chengdongqing.wechat.core.common.util

import java.util.UUID

/**
 * 生成唯一标识符（ID）
 */
fun randomUUID() = UUID.randomUUID().toString().replace("-", "")

/**
 * 根据小数计算百分比
 */
fun Float.toPercent() = "${(this * 100).toInt()}%"