package top.chengdongqing.wechat2.ui.components

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat2.data.model_1.P2pMode

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionWrapper(
    mode: P2pMode,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val permissions = remember(mode) { getPermissionsForMode(mode) }
    val permissionState = rememberMultiplePermissionsState(permissions)
    var isHardwareReady by remember { mutableStateOf(false) }

    val bluetoothAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    // 封装检查逻辑
    val checkHardwareStatus = {
        val btReady = if (mode == P2pMode.Bluetooth) {
            bluetoothAdapter?.isEnabled == true
        } else true

        val wifiReady = if (mode != P2pMode.Bluetooth) {
            wifiManager?.isWifiEnabled == true
        } else true

        isHardwareReady = btReady && wifiReady
    }

    // 监听生命周期（从设置页回来时触发刷新）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mode) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkHardwareStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 注册广播接收器，实现秒级实时响应
    DisposableEffect(mode) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                checkHardwareStatus()
            }
        }
        val filter = IntentFilter().apply {
            if (mode == P2pMode.Bluetooth) addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            else addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    if (isHardwareReady && permissionState.allPermissionsGranted) {
        content()
    } else {
        PermissionGuideScreen(
            mode = mode,
            onAction = {
                if (mode == P2pMode.Bluetooth) {
                    if (bluetoothAdapter == null) {
                        context.showToast("此设备不支持蓝牙")
                    } else if (!bluetoothAdapter.isEnabled) {
                        context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))

                    }

                } else {
                    if (wifiManager == null) {
                        context.showToast("此设备不支持Wi-Fi")
                    } else if (wifiManager.isWifiEnabled) {
                        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    }
                    permissionState.launchMultiplePermissionRequest()
                }
            }
        )
    }
}

@Composable
fun PermissionGuideScreen(
    mode: P2pMode,
    onAction: () -> Unit
) {
    val (icon, title, desc) = when (mode) {
        P2pMode.WifiLan -> Triple(
            Icons.Default.Wifi,
            "局域网传输",
            "需要 Wi-Fi 状态权限以发现同一路由器下的伙伴。"
        )

        P2pMode.WifiDirect -> Triple(
            Icons.Default.WifiTethering, // 或者使用自定义 WFD 图标
            "Wi-Fi 直连 (快传)",
            "需要在没有路由器的情况下搜索附近的手机，这需要精确位置和附近设备权限。"
        )

        P2pMode.Bluetooth -> Triple(
            Icons.Default.Bluetooth,
            "蓝牙传输",
            "需要扫描并连接附近的蓝牙设备，请授权蓝牙相关权限。"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 动态图标
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("授予权限并开始")
        }

        // 针对 Wi-Fi Direct 的特殊提示
        if (mode == P2pMode.WifiDirect) {
            Text(
                text = "提示：Wi-Fi 直连在某些设备上还需手动开启 GPS",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

private fun getPermissionsForMode(mode: P2pMode): List<String> {
    return when (mode) {
        P2pMode.WifiLan -> listOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE
        )

        P2pMode.WifiDirect -> {
            val list = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            list
        }

        P2pMode.Bluetooth -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                listOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }
}