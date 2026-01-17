package top.chengdongqing.wechat.core.util

import java.util.UUID

fun randomUUID(): String = UUID.randomUUID().toString().replace("-", "")