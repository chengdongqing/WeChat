package top.chengdongqing.wechat.core.util

import android.net.Uri
import java.util.UUID

/**
 * 生成唯一标识符（ID）
 */
fun randomUUID() = UUID.randomUUID().toString().replace("-", "")

/**
 * 根据小数计算百分比
 */
fun Float.toPercent() = "${(this * 100).toInt()}%"

/**
 * 字符串编解码
 */
fun String.encode(): String = Uri.encode(this)
fun String.decode(): String = Uri.decode(this)

/**
 * 字符串转 MD5 十六进制
 */
fun String.toMD5Hex(): String {
    val md = java.security.MessageDigest.getInstance("MD5")
    val digest = md.digest(this.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}