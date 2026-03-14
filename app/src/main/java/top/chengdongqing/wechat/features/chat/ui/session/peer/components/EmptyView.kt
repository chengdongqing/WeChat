package top.chengdongqing.wechat.features.chat.ui.session.peer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.data.network.connection.ConnectionMode

@Composable
fun EmptyView(mode: ConnectionMode) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = if (mode == ConnectionMode.Bluetooth) Icons.Default.Bluetooth else Icons.Default.Wifi,
            contentDescription = null,
            tint = WeTheme.colorScheme.textSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp),
        )
        Text(text = "未找到附近设备", fontSize = 14.sp, color = WeTheme.colorScheme.textSecondary)
        Text(
            text = if (mode == ConnectionMode.Bluetooth) "请确保对方已开启蓝牙" else "请确保对方已开启 Wi-Fi Direct",
            fontSize = 12.sp,
            color = WeTheme.colorScheme.textSecondary.copy(alpha = 0.6f),
        )
    }
}