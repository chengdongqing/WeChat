package top.chengdongqing.wechat.core.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * 字节数组转 MD5 十六进制
 *
 * 性能优于 joinToString + String.format
 */
fun ByteArray.toMD5Hex(): String {
    val hexChars = "0123456789abcdef"
    val result = StringBuilder(size * 2)
    forEach { byte ->
        val i = byte.toInt() and 0xFF
        result.append(hexChars[i shr 4])
        result.append(hexChars[i and 0x0F])
    }
    return result.toString()
}

/**
 * 字符串转 MD5 字符数组
 */
fun String.toMD5Bytes(): ByteArray {
    val md = MessageDigest.getInstance("MD5")
    return md.digest(this.toByteArray())
}

/**
 * 字符串转 MD5 十六进制
 */
fun String.toMD5Hex(): String {
    return toMD5Bytes().toMD5Hex()
}

/**
 * 流式计算文件 MD5 十六进制
 */
suspend fun File.toMD5Hex(): String = withContext(Dispatchers.IO) {
    if (!exists() || !isFile) return@withContext ""

    val digest = MessageDigest.getInstance("MD5")
    val buffer = ByteArray(8192)

    try {
        FileInputStream(this@toMD5Hex).use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        digest.digest().toMD5Hex()
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}