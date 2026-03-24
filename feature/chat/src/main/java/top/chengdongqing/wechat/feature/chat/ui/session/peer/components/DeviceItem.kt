package top.chengdongqing.wechat.feature.chat.ui.session.peer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.chat.domain.model.PeerDevice

@Composable
fun DeviceItem(
    device: PeerDevice,
    isConnecting: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(WeTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when (device) {
                    is PeerDevice.Bluetooth ->
                        if (device.isPaired) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth

                    is PeerDevice.WiFiDirect -> Icons.Default.Wifi
                },
                contentDescription = null,
                tint = WeTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                fontSize = 16.sp,
                color = WeTheme.colorScheme.textPrimary,
            )
            Text(
                text = when {
                    device.isPaired -> stringResource(R.string.conn_device_status_paired)
                    device.signalStrength != 0 -> signalStrengthText(device.signalStrength)
                    else -> ""
                },
                fontSize = 12.sp,
                color = if (device.isPaired) WeTheme.colorScheme.primary else WeTheme.colorScheme.textSecondary,
            )
        }

        if (isConnecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = WeTheme.colorScheme.primary,
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun signalStrengthText(rssi: Int) = when {
    rssi >= -60 -> stringResource(R.string.conn_signal_excellent)
    rssi >= -70 -> stringResource(R.string.conn_signal_good)
    rssi >= -80 -> stringResource(R.string.conn_signal_fair)
    else -> stringResource(R.string.conn_signal_weak)
}