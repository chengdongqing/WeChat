package top.chengdongqing.wechat.features.settings.ui.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import top.chengdongqing.wechat.core.designsystem.components.menu.SettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.SettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.SettingValue
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.features.settings.navigation.SettingsRoute

@Composable
fun NotificationSettingsScreen(navController: NavHostController, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            WeTopBar(title = "通知", onBack = onBack)
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    state = rememberScrollState(),
                    overscrollEffect = rememberBounceOverscrollEffect()
                )
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingGroup {
                SettingItem(
                    label = "消息通知",
                    showArrow = false
                ) {
                    WeSwitch(checked = true)
                }
                SettingItem(
                    label = "语音和视频通话通知",
                    showArrow = false,
                    showDivider = false
                ) {
                    WeSwitch(checked = true)
                }
            }
            SettingItem(
                label = "通知显示内容",
                showDivider = false,
                onClick = {
                    navController.navigate(SettingsRoute.NotificationDisplaySettings.route)
                }
            )
            SettingGroup("声音与振动") {
                SettingItem(
                    label = "消息通知",
                    onClick = {}
                ) {
                    SettingValue("前往系统设置")
                }
                SettingItem(
                    label = "语音和视频通话通知",
                    onClick = {}
                ) {
                    SettingValue("前往系统设置")
                }
                SettingItem(
                    label = "聊天界面中的新消息通知",
                    showDivider = false,
                    onClick = {
                        navController.navigate(SettingsRoute.InChatNotificationSettings.route)
                    }
                )
            }
            SettingGroup("提示音和铃声") {
                SettingItem(
                    label = "消息提示音",
                    onClick = {
                        navController.navigate(SettingsRoute.NotificationSoundSetting.route)
                    }
                ) {
                    SettingValue("跟随系统")
                }
                SettingItem(
                    label = "来电铃声",
                    onClick = {}
                ) {
                    SettingValue("Lullaby")
                }
                SettingItem(
                    label = "呼叫我时朋友也可听见我的来电铃声",
                    showArrow = false,
                    showDivider = false
                ) {
                    WeSwitch(checked = true)
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}