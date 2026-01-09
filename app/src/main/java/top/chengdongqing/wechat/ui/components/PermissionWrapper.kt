package top.chengdongqing.wechat.ui.components

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import top.chengdongqing.wechat.core.util.PermissionUtils
import top.chengdongqing.wechat.data.model.P2pMode

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionWrapper(
    mode: P2pMode,
    content: @Composable () -> Unit
) {
    // 1. 根据当前选择的模式，获取对应的权限列表
    val permissions = remember(mode) {
        PermissionUtils.getPermissionsForMode(mode)
    }

    // 2. 记住这些权限的状态
    val permissionState = rememberMultiplePermissionsState(permissions)

    // 3. 逻辑分发
    if (permissionState.allPermissionsGranted) {
        // 只有全部授权，才显示核心聊天/传输界面
        content()
    } else {
        // 显示针对性引导
        PermissionGuideScreen(
            mode = mode,
            onAction = {
                permissionState.launchMultiplePermissionRequest()
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
        P2pMode.WIFI_LAN -> Triple(
            Icons.Default.Wifi,
            "局域网传输",
            "需要 Wi-Fi 状态权限以发现同一路由器下的伙伴。"
        )

        P2pMode.WIFI_DIRECT -> Triple(
            Icons.Default.WifiTethering, // 或者使用自定义 WFD 图标
            "Wi-Fi 直连 (快传)",
            "需要在没有路由器的情况下搜索附近的手机，这需要精确位置和附近设备权限。"
        )

        P2pMode.BLUETOOTH -> Triple(
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
        if (mode == P2pMode.WIFI_DIRECT) {
            Text(
                text = "提示：Wi-Fi 直连在某些设备上还需手动开启 GPS",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}