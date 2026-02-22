package top.chengdongqing.wechat.core.util

import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface

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