package top.chengdongqing.wechat.feature.chat.ui.session.peer

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.chat.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

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
        Text(
            text = stringResource(R.string.conn_empty_no_devices),
            color = WeTheme.colorScheme.textSecondary,
            fontSize = 14.sp
        )
        Text(
            text = if (mode == ConnectionMode.Bluetooth) {
                stringResource(R.string.conn_empty_ensure_bluetooth)
            } else {
                stringResource(R.string.conn_empty_ensure_wifi_direct)
            },
            color = WeTheme.colorScheme.textSecondary.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}
