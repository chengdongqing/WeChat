package top.chengdongqing.wechat.core.util

/**
 * 将 ByteArray 转为可读的十六进制字符串
 */
fun ByteArray.toHexString(): String = joinToString(" ") {
    "%02X".format(it)
}

/**
 * 将单个 Byte 转为两位十六进制字符串
 */
fun Byte.toHexByte(): String = "%02X".format(this)

/**
 * 按指定大小分块
 */
fun ByteArray.chunked(size: Int): List<ByteArray> = (indices step size).map { i ->
    copyOfRange(i, minOf(i + size, this.size))
}