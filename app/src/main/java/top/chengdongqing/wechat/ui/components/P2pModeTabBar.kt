package top.chengdongqing.wechat.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.data.model.P2pMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun P2pModeTabBar(
    selectedMode: P2pMode,
    onModeSelected: (P2pMode) -> Unit
) {
    val modes = P2pMode.entries

    Column {
        CenterAlignedTopAppBar(
            title = { Text("P2P 传输 - ${selectedMode.label}") }
        )

        // Tab 切换行
        SecondaryTabRow(
            selectedTabIndex = selectedMode.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(
                        selectedTabIndex = selectedMode.ordinal
                    )
                )
            }
        ) {
            modes.forEach { mode ->
                Tab(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    text = {
                        Text(
                            text = mode.label.replace("模式", ""), // 简化显示
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    icon = {
                        val icon = when (mode) {
                            P2pMode.WifiLan -> Icons.Default.Wifi
                            P2pMode.WifiDirect -> Icons.Default.WifiTethering
                            P2pMode.Bluetooth -> Icons.Default.Bluetooth
                        }
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                )
            }
        }
    }
}