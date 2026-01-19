package top.chengdongqing.wechat.core.utils

import java.util.UUID

fun randomUUID(): String = UUID.randomUUID().toString().replace("-", "")