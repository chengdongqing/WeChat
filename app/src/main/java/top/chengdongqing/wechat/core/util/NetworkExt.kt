package top.chengdongqing.wechat.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun rememberWifiConnected(): Boolean {
    val context = LocalContext.current

    return produceState(initialValue = false) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // 当任何网络可用时，检查是否为 Wi-Fi
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                if (isWifi) value = true
            }

            override fun onLost(network: Network) {
                // 当网络丢失时，检查当前是否还有其他 Wi-Fi 连接
                // 这里的逻辑可以根据需求简化，简单起见直接置为 false
                value = false
            }
        }

        // 定义网络请求条件：必须是 Wi-Fi
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        // 注册监听
        connectivityManager.registerNetworkCallback(request, callback)

        // 初始化检查（注册回调前可能已经连上了）
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        value = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        // 当 Composable 退出时，自动注销，防止内存泄漏
        awaitDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.value
}