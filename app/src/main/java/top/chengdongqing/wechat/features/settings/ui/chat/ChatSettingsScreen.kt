package top.chengdongqing.wechat.features.settings.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun ChatSettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            WeTopBar(
                title = stringResource(R.string.settings_chat),
                onBack = onBack
            )
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
                label = stringResource(R.string.chat_settings_background),
                showDivider = false,
                onClick = {}
            )
            WeSettingGroup {
                WeSettingItem(
                    label = stringResource(R.string.chat_settings_earpiece),
                    showArrow = false
                ) {
                    WeSwitch()
                }
                WeSettingItem(
                    label = stringResource(R.string.chat_settings_send_button),
                    showArrow = false,
                    showDivider = false,
                ) {
                    WeSwitch(checked = true)
                }
            }
            WeSettingItem(
                label = stringResource(R.string.chat_settings_e2e),
                description = stringResource(R.string.chat_settings_e2e_desc),
                showArrow = false,
                showDivider = false,
                height = 68.dp
            ) {
                WeSwitch(checked = true)
            }
        }
    }
}