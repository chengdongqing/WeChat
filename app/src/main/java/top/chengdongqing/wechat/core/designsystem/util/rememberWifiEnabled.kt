package top.chengdongqing.wechat.core.designsystem.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService

@Composable
fun rememberWifiEnabled(): Boolean {
    val context = LocalContext.current

    return produceState(
        initialValue = context.getSystemService<WifiManager>()?.isWifiEnabled == true
    ) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != WifiManager.WIFI_STATE_CHANGED_ACTION) return
                val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)
                value = wifiState == WifiManager.WIFI_STATE_ENABLED
            }
        }

        context.registerReceiver(receiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))

        awaitDispose { context.unregisterReceiver(receiver) }
    }.value
}