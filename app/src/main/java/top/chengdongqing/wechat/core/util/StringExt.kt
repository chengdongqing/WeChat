package top.chengdongqing.wechat.core.util

import java.util.UUID

/**
 * 生成唯一标识符（ID）
 */
fun randomUUID(): String = UUID.randomUUID().toString().replace("-", "")