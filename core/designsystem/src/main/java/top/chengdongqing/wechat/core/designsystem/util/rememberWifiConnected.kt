package top.chengdongqing.wechat.core.designsystem.util

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext

@Composable
@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun rememberWifiConnected(): Boolean {
    val context = LocalContext.current

    return produceState(initialValue = false) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
            override fun onAvailable(network: Network) {
                // 当任何网络可用时，检查是否为 Wi-Fi
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                if (isWifi) value = true
            }

            override fun onLost(network: Network) {
                // 当网络丢失时
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