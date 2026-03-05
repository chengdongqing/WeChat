package top.chengdongqing.wechat.features.settings.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun ChatSettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            WeTopBar(title = "聊天", onBack = onBack)
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeSettingItem(
                label = "聊天背景",
                showDivider = false,
                onClick = {}
            )
            WeSettingGroup {
                WeSettingItem(
                    label = "使用听筒播放语音消息",
                    showArrow = false
                ) {
                    WeSwitch()
                }
                WeSettingItem(
                    label = "使用独立的发送按钮",
                    showArrow = false,
                    showDivider = false,
                ) {
                    WeSwitch(checked = true)
                }
            }
            WeSettingItem(
                label = "端到端加密",
                description = "只要一方开启，双方将自动建立加密连接。",
                showArrow = false,
                showDivider = false,
                height = 68.dp
            ) {
                WeSwitch(checked = true)
            }
        }
    }
}