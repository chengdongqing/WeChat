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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingValue
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.util.navigateToAppSettings
import top.chengdongqing.wechat.features.settings.navigation.SettingsRoute

@Composable
fun NotificationSettingsScreen(
    navController: NavHostController,
    onBack: () -> Unit
) {
    val context = LocalContext.current

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
            WeSettingGroup {
                WeSettingItem(
                    label = "消息通知",
                    showArrow = false
                ) {
                    WeSwitch(checked = true)
                }
                WeSettingItem(
                    label = "语音和视频通话通知",
                    showArrow = false,
                    showDivider = false
                ) {
                    WeSwitch(checked = true)
                }
            }
            WeSettingItem(
                label = "通知显示内容",
                description = "显示朋友和群聊的名称、头像、消息内容",
                showDivider = false,
                height = 68.dp,
                onClick = {
                    navController.navigate(SettingsRoute.NotificationDisplaySetting.route)
                }
            )
            WeSettingGroup("声音与振动") {
                WeSettingItem(
                    label = "消息通知",
                    onClick = {
                        context.navigateToAppSettings(true)
                    }
                ) {
                    WeSettingValue("前往系统设置")
                }
                WeSettingItem(
                    label = "语音和视频通话通知",
                    onClick = {
                        context.navigateToAppSettings(true)
                    }
                ) {
                    WeSettingValue("前往系统设置")
                }
                WeSettingItem(
                    label = "聊天界面中的新消息通知",
                    showDivider = false,
                    onClick = {
                        navController.navigate(SettingsRoute.InChatNotificationSetting.route)
                    }
                )
            }
            WeSettingGroup("提示音和铃声") {
                WeSettingItem(
                    label = "消息提示音",
                    onClick = {
                        navController.navigate(SettingsRoute.NotificationSoundSetting.route)
                    }
                ) {
                    WeSettingValue("跟随系统")
                }
                WeSettingItem(
                    label = "来电铃声",
                    onClick = {}
                ) {
                    WeSettingValue("Lullaby")
                }
                WeSettingItem(
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