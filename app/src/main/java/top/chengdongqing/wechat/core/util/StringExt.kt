package top.chengdongqing.wechat.core.util

import android.net.Uri
import java.security.MessageDigest
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
 * 字符串转 MD5 字节数组
 */
fun String.toMD5Bytes(): ByteArray {
    val md = MessageDigest.getInstance("MD5")
    return md.digest(this.toByteArray())
}

/**
 * 字符串转 MD5 十六进制
 */
fun String.toMD5Hex(): String {
    return toMD5Bytes().joinToString("") { "%02x".format(it) }
}