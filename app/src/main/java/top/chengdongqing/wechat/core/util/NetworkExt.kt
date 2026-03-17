package top.chengdongqing.wechat.core.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL

/**
 * 获取本机IP
 */
fun getLocalIpAddress(): String? {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in interfaces) {
            // 过滤掉回环接口(127.0.0.1)和未开启的接口
            if (intf.isLoopback || !intf.isUp) continue
            // 重点过滤：通常多播和 P2P 发生在 wlan0 接口上
            if (!intf.name.contains("wlan")) continue

            val addrs = intf.inetAddresses
            for (addr in addrs) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress
                }
            }
        }
    } catch (e: Exception) {
        Log.e("NetworkExt", "获取IP失败", e)
    }
    return null
}

/**
 * 下载头像到本地
 */
suspend fun downloadAvatar(url: String, targetFile: File): Result<File> =
    withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(url)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 10000
                doInput = true
            }

            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP Error: ${connection.responseCode}")
            }

            // 确保目录存在
            targetFile.parentFile?.mkdirs()

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }

            connection.disconnect()
            targetFile
        }
    }

