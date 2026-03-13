package top.chengdongqing.wechat.features.chat.ui.session.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.popup.WePopup
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun BluetoothDeviceOverlay(
    visible: Boolean,
    userId: String,
    onConnected: () -> Unit,
    onClose: () -> Unit,
    viewModel: BluetoothDeviceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    DisposableEffect(visible) {
        if (!visible) return@DisposableEffect onDispose {}

        // 让本机120秒内可被发现
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
        }
        context.startActivity(discoverableIntent)

        // 开始扫描
        viewModel.startScan()

        /**
         * 处理扫描结果
         */
        val receiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(ctx: Context, intent: Intent) {
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
                        viewModel.addDiscoveredDevice(device, rssi)
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        viewModel.onDiscoveryFinished()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
            viewModel.stopScan()
        }
    }

    // 连接成功后自动关闭
    DisposableEffect(state.connectingDeviceAddress) {
        if (state.connectingDeviceAddress == null && state.error == null) {
            // 有过连接动作且成功完成
            onClose()
        }
        onDispose {}
    }

    WePopup(
        visible = visible,
        padding = PaddingValues(vertical = 16.dp),
        title = "选择蓝牙设备",
        onClose = onClose
    ) {
        LazyColumn {
            // 扫描状态指示
            item {
                ScanningIndicator(isScanning = state.isScanning)
            }

            // 错误提示
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

            // 已配对设备
            if (state.pairedDevices.isNotEmpty()) {
                item {
                    SectionHeader(title = "已配对设备")
                }
                items(state.pairedDevices, key = { it.device.address }) { device ->
                    DeviceItem(
                        device = device,
                        isConnecting = state.connectingDeviceAddress == device.device.address,
                        onClick = {
                            viewModel.connectDevice(device, userId)
                            onConnected()
                        }
                    )
                    WeDivider(modifier = Modifier.padding(start = 64.dp))
                }
            }

            // 附近设备
            if (state.nearbyDevices.isNotEmpty()) {
                item {
                    SectionHeader(title = "附近设备")
                }
                items(state.nearbyDevices, key = { it.device.address }) { device ->
                    DeviceItem(
                        device = device,
                        isConnecting = state.connectingDeviceAddress == device.device.address,
                        onClick = {
                            viewModel.connectDevice(device, userId)
                            onConnected()
                        }
                    )
                    WeDivider(modifier = Modifier.padding(start = 64.dp))
                }
            }

            // 没有找到任何设备
            if (state.pairedDevices.isEmpty() && state.nearbyDevices.isEmpty() && !state.isScanning) {
                item {
                    EmptyView()
                }
            }

            item { Spacer(modifier = Modifier.height(120.dp)) }
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
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 4.dp
        )
    )
}

@Composable
private fun DeviceItem(
    device: ScannedDevice,
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
        // 蓝牙图标
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(WeTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (device.isPaired)
                    Icons.Default.BluetoothConnected
                else
                    Icons.Default.Bluetooth,
                contentDescription = null,
                tint = WeTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 设备名和地址
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = WeTheme.colorScheme.textPrimary
            )
            if (!device.isPaired && device.rssi != 0) {
                Text(
                    text = signalStrengthText(device.rssi),
                    fontSize = 12.sp,
                    color = WeTheme.colorScheme.textSecondary
                )
            } else if (device.isPaired) {
                Text(
                    text = "已配对",
                    fontSize = 12.sp,
                    color = WeTheme.colorScheme.primary
                )
            }
        }

        // 连接中 / 已配对标记
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
private fun EmptyView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Bluetooth,
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
            text = "请确保对方已开启蓝牙",
            fontSize = 12.sp,
            color = WeTheme.colorScheme.textSecondary.copy(alpha = 0.6f)
        )
    }
}

private fun signalStrengthText(rssi: Int): String = when {
    rssi >= -60 -> "信号极强"
    rssi >= -70 -> "信号良好"
    rssi >= -80 -> "信号一般"
    else -> "信号较弱"
}