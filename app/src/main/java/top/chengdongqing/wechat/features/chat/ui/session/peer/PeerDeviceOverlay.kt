package top.chengdongqing.wechat.features.chat.ui.session.peer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.popup.WePopup
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.data.network.connection.ConnectionMode
import top.chengdongqing.wechat.features.chat.domain.model.PeerDevice
import top.chengdongqing.wechat.features.chat.domain.model.WiFiDirectRole

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PeerDeviceOverlay(
    visible: Boolean,
    userId: String,
    mode: ConnectionMode,
    onConnected: () -> Unit,
    onClose: () -> Unit,
) {
    val viewModel: PeerDeviceViewModel = when (mode) {
        ConnectionMode.Bluetooth -> hiltViewModel<BluetoothPeerViewModel>()
        ConnectionMode.WiFiDirect -> hiltViewModel<WiFiDirectPeerViewModel>()
        else -> return
    }

    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
    )

    DisposableEffect(visible) {
        if (!visible) return@DisposableEffect onDispose {}

        if (mode == ConnectionMode.WiFiDirect) {
            if (!permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            }
        } else {
            viewModel.startScan()
        }

        // 根据模式注册对应广播
        val receiver = when (mode) {
            ConnectionMode.Bluetooth -> buildBluetoothReceiver(viewModel as BluetoothPeerViewModel) {
                context.startActivity(
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }

            ConnectionMode.WiFiDirect -> buildWifiDirectReceiver(
                context,
                viewModel as WiFiDirectPeerViewModel
            )
        }

        val filter = when (mode) {
            ConnectionMode.Bluetooth -> IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }

            ConnectionMode.WiFiDirect -> IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            receiver.let { runCatching { context.unregisterReceiver(it) } }
            viewModel.stopScan()
        }
    }

    WePopup(
        visible = visible,
        padding = PaddingValues(vertical = 16.dp),
        title = when (mode) {
            ConnectionMode.Bluetooth -> "选择蓝牙设备"
            ConnectionMode.WiFiDirect -> "选择 Wi-Fi Direct 设备"
        },
        onClose = onClose
    ) {
        LazyColumn {
            // WiFi Direct 模式且还没选角色，先显示选择按钮
            if (mode == ConnectionMode.WiFiDirect) {
                val wfdViewModel = viewModel as WiFiDirectPeerViewModel
                val role = state.role

                if (role == WiFiDirectRole.None) {
                    item {
                        WiFiDirectRoleSelector(
                            onCreateGroup = { wfdViewModel.startAsOwner() },
                            onJoinGroup = { wfdViewModel.startAsClient() }
                        )
                    }
                    return@LazyColumn
                }

                // Owner 模式显示等待界面
                if (role == WiFiDirectRole.Owner) {
                    item { OwnerWaitingView() }
                    return@LazyColumn
                }
            }

            item { ScanningIndicator(isScanning = state.isScanning) }

            state.error?.let { error ->
                item {
                    Text(
                        text = error,
                        color = WeTheme.colorScheme.danger,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            if (state.pairedDevices.isNotEmpty()) {
                item { SectionHeader("已配对设备") }
                items(state.pairedDevices, key = { "bonded_" + it.id }) { device ->
                    DeviceItem(
                        device = device,
                        isConnecting = state.connectingDeviceId == device.id,
                        onClick = {
                            viewModel.connectDevice(device, userId) {
                                onConnected()
                                onClose()
                            }
                        }
                    )
                    WeDivider(modifier = Modifier.padding(start = 64.dp))
                }
            }

            if (state.nearbyDevices.isNotEmpty()) {
                item { SectionHeader("附近设备") }
                items(state.nearbyDevices, key = { it.id }) { device ->
                    DeviceItem(
                        device = device,
                        isConnecting = state.connectingDeviceId == device.id,
                        onClick = {
                            viewModel.connectDevice(device, userId) {
                                onConnected()
                                onClose()
                            }
                        }
                    )
                    WeDivider(modifier = Modifier.padding(start = 64.dp))
                }
            }

            if (state.pairedDevices.isEmpty()
                && state.nearbyDevices.isEmpty()
                && !state.isScanning
            ) {
                item { EmptyView(mode) }
            }

            item { Spacer(modifier = Modifier.height(120.dp)) }
        }
    }
}

private fun buildBluetoothReceiver(
    viewModel: BluetoothPeerViewModel,
    onStartDiscoverable: () -> Unit
): BroadcastReceiver {
    onStartDiscoverable()
    return object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    } ?: return
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0).toInt()
                    viewModel.onClassicDeviceFound(device, rssi)
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    viewModel.onDiscoveryFinished()
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun buildWifiDirectReceiver(
    context: Context,
    viewModel: WiFiDirectPeerViewModel
): BroadcastReceiver {
    return object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) return
            val p2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
            val channel = p2pManager.initialize(context, ctx.mainLooper, null)
            p2pManager.requestPeers(channel) { peers: WifiP2pDeviceList ->
                viewModel.onPeersChanged(peers.deviceList.toList())
            }
        }
    }
}

@Composable
private fun ScanningIndicator(isScanning: Boolean) {
    if (!isScanning) return
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .size(8.dp)
                .clip(CircleShape)
                .background(WeTheme.colorScheme.primary)
        )
        Text(
            text = "正在搜索附近设备...",
            fontSize = 13.sp,
            color = WeTheme.colorScheme.textSecondary
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        color = WeTheme.colorScheme.textSecondary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun DeviceItem(
    device: PeerDevice,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(WeTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (device) {
                    is PeerDevice.Bluetooth ->
                        if (device.isPaired) Icons.Default.BluetoothConnected
                        else Icons.Default.Bluetooth

                    is PeerDevice.WiFiDirect -> Icons.Default.Wifi
                },
                contentDescription = null,
                tint = WeTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = WeTheme.colorScheme.textPrimary
            )
            Text(
                text = when {
                    device.isPaired -> "已配对"
                    device.signalStrength != 0 -> signalStrengthText(device.signalStrength)
                    else -> ""
                },
                fontSize = 12.sp,
                color = if (device.isPaired)
                    WeTheme.colorScheme.primary
                else
                    WeTheme.colorScheme.textSecondary
            )
        }

        if (isConnecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = WeTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun EmptyView(mode: ConnectionMode) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = when (mode) {
                ConnectionMode.Bluetooth -> Icons.Default.Bluetooth
                else -> Icons.Default.Wifi
            },
            contentDescription = null,
            tint = WeTheme.colorScheme.textSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "未找到附近设备",
            fontSize = 14.sp,
            color = WeTheme.colorScheme.textSecondary
        )
        Text(
            text = when (mode) {
                ConnectionMode.Bluetooth -> "请确保对方已开启蓝牙"
                else -> "请确保对方已开启 Wi-Fi Direct"
            },
            fontSize = 12.sp,
            color = WeTheme.colorScheme.textSecondary.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun WiFiDirectRoleSelector(
    onCreateGroup: () -> Unit,
    onJoinGroup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "选择连接方式",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = WeTheme.colorScheme.textPrimary
        )
        Text(
            text = "一台设备创建群组，另一台加入群组",
            fontSize = 13.sp,
            color = WeTheme.colorScheme.textSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RoleButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.WifiTethering,
                label = "创建群组",
                description = "让对方来连接你",
                onClick = onCreateGroup
            )
            RoleButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Wifi,
                label = "加入群组",
                description = "搜索并连接对方",
                onClick = onJoinGroup
            )
        }
    }
}

@Composable
private fun RoleButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(WeTheme.colorScheme.primary.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WeTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = WeTheme.colorScheme.textPrimary
        )
        Text(
            text = description,
            fontSize = 12.sp,
            color = WeTheme.colorScheme.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OwnerWaitingView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        Icon(
            imageVector = Icons.Default.WifiTethering,
            contentDescription = null,
            tint = WeTheme.colorScheme.primary,
            modifier = Modifier
                .size(56.dp)
                .scale(scale)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "群组已创建",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = WeTheme.colorScheme.textPrimary
        )
        Text(
            text = "等待对方选择「加入群组」连接你",
            fontSize = 13.sp,
            color = WeTheme.colorScheme.textSecondary
        )
    }
}

private fun signalStrengthText(rssi: Int) = when {
    rssi >= -60 -> "信号极强"
    rssi >= -70 -> "信号良好"
    rssi >= -80 -> "信号一般"
    else -> "信号较弱"
}