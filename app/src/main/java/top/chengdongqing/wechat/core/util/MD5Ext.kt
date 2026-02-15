package top.chengdongqing.wechat.core.util

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

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

/**
 * 流式计算文件 MD5 十六进制
 *
 * 磁盘顺序读 + MD5 计算，在 Android 设备上约 200-400 MB/s。
 */
fun File.toMD5Hex(): String {
    val digest = MessageDigest.getInstance("MD5")
    val chunkSize = 256 * 1024
    val buffer = ByteArray(chunkSize)

    FileInputStream(this).buffered(chunkSize).use { fis ->
        while (true) {
            val bytesRead = fis.read(buffer)
            if (bytesRead == -1) break
            digest.update(buffer, 0, bytesRead)
        }
    }

    return digest.digest().joinToString("") { "%02x".format(it) }
}