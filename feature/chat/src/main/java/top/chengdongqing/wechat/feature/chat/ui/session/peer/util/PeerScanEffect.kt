package top.chengdongqing.wechat.feature.chat.ui.session.peer.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.feature.chat.ui.session.peer.BluetoothPeerViewModel
import top.chengdongqing.wechat.feature.chat.ui.session.peer.PeerDeviceViewModel
import top.chengdongqing.wechat.feature.chat.ui.session.peer.WiFiDirectPeerViewModel

/**
 * 管理设备扫描的生命周期：overlay 可见时注册广播并开始扫描，不可见时自动清理。
 *
 * - Bluetooth：立即开始扫描，并请求让本机对外可见
 * - WiFiDirect：先检查权限，再启动 P2P 对等发现
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PeerScanEffect(
    mode: ConnectionMode,
    viewModel: PeerDeviceViewModel
) {
    val context = LocalContext.current

    val permissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
    )

    DisposableEffect(Unit) {
        // 根据模式决定是否需要先申请权限，Bluetooth 直接扫描
        if (mode == ConnectionMode.WiFiDirect) {
            if (!permissionState.status.isGranted) permissionState.launchPermissionRequest()
        } else {
            viewModel.startScan()
        }

        val receiver = buildReceiver(mode, viewModel, context)
        val filter = buildIntentFilter(mode)

        // RECEIVER_NOT_EXPORTED：仅接收应用内广播，防止外部伪造事件
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            viewModel.stopScan()
        }
    }
}

private fun buildReceiver(
    mode: ConnectionMode,
    viewModel: PeerDeviceViewModel,
    context: Context,
): BroadcastReceiver = when (mode) {
    ConnectionMode.Bluetooth -> buildBluetoothReceiver(
        viewModel = viewModel as BluetoothPeerViewModel,
        onStartDiscoverable = {
            // 向系统请求让本机蓝牙在 120 秒内对外可见，供对方扫描发现
            context.startActivity(
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    )

    ConnectionMode.WiFiDirect -> buildWifiDirectReceiver(
        context,
        viewModel as WiFiDirectPeerViewModel
    )

    else -> throw IllegalArgumentException()
}

private fun buildIntentFilter(mode: ConnectionMode): IntentFilter = when (mode) {
    ConnectionMode.Bluetooth -> IntentFilter().apply {
        addAction(BluetoothDevice.ACTION_FOUND)                 // 发现新设备
        addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)   // 扫描结束
    }

    ConnectionMode.WiFiDirect -> IntentFilter(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)

    else -> throw IllegalArgumentException()
}