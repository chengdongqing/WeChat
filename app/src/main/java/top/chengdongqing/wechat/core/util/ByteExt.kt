package top.chengdongqing.wechat.core.util

/**
 * 将 ByteArray 转为可读的十六进制字符串，便于日志调试
 */
fun ByteArray.toHexString(): String = joinToString(" ") {
    "%02X".format(it)
}

/**
 * 将单个 Byte 转为两位十六进制字符串
 */
fun Byte.toHexByte(): String = "%02X".format(this)