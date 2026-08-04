package top.chengdongqing.wechat.core.connectivity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService

@Composable
fun rememberBluetoothEnabled(): Boolean {
    val context = LocalContext.current
    val adapter = context.getSystemService<BluetoothManager>()?.adapter

    return produceState(initialValue = adapter?.isEnabled == true) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
                value = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) ==
                        BluetoothAdapter.STATE_ON
            }
        }
        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        awaitDispose { context.unregisterReceiver(receiver) }
    }.value
}

@Composable
@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun rememberWifiConnected(): Boolean {
    val context = LocalContext.current

    return produceState(initialValue = false) {
        val manager = context.getSystemService<ConnectivityManager>() ?: return@produceState
        val callback = object : ConnectivityManager.NetworkCallback() {
            @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
            override fun onAvailable(network: Network) {
                value = manager.getNetworkCapabilities(network)
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }

            @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
            override fun onLost(network: Network) {
                value = manager.activeNetwork?.let(manager::getNetworkCapabilities)
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        manager.registerNetworkCallback(request, callback)
        value = manager.activeNetwork?.let(manager::getNetworkCapabilities)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        awaitDispose { manager.unregisterNetworkCallback(callback) }
    }.value
}

@Composable
fun rememberWifiEnabled(): Boolean {
    val context = LocalContext.current

    return produceState(
        initialValue = context.getSystemService<WifiManager>()?.isWifiEnabled == true
    ) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != WifiManager.WIFI_STATE_CHANGED_ACTION) return
                value = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1) ==
                        WifiManager.WIFI_STATE_ENABLED
            }
        }
        context.registerReceiver(receiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
        awaitDispose { context.unregisterReceiver(receiver) }
    }.value
}
