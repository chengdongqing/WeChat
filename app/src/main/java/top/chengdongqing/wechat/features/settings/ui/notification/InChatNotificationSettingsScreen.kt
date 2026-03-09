package top.chengdongqing.wechat.features.settings.ui.notification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun InChatNotificationSettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            WeTopBar(
                title = stringResource(R.string.notification_in_chat),
                onBack = onBack
            )
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            WeSettingGroup {
                WeSettingItem(
                    label = stringResource(R.string.notification_in_chat_sound),
                    showArrow = false
                ) {
                    WeSwitch(checked = true)
                }
                WeSettingItem(
                    label = stringResource(R.string.notification_in_chat_vibration),
                    showArrow = false,
                    showDivider = false
                ) {
                    WeSwitch(checked = true)
                }
            }

            SettingHint()
        }
    }
}

@Composable
private fun SettingHint() {
    val textStyle = TextStyle(
        color = WeTheme.colorScheme.textSecondary,
        fontSize = 13.sp
    )

    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = stringResource(R.string.notification_in_chat_hint),
            style = textStyle
        )
        Spacer(modifier = Modifier.height(26.dp))
        Text(
            text = stringResource(R.string.notification_in_chat_hint_item1),
            style = textStyle
        )
        Text(
            text = stringResource(R.string.notification_in_chat_hint_item2),
            style = textStyle
        )
    }
}