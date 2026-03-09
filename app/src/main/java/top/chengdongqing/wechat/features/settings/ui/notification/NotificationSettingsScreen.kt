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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import top.chengdongqing.wechat.R
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
            WeTopBar(title = stringResource(R.string.settings_notifications), onBack = onBack)
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
                    label = stringResource(R.string.notification_msg),
                    showArrow = false
                ) {
                    WeSwitch(checked = true)
                }
                WeSettingItem(
                    label = stringResource(R.string.notification_call),
                    showArrow = false,
                    showDivider = false
                ) {
                    WeSwitch(checked = true)
                }
            }
            WeSettingItem(
                label = stringResource(R.string.notification_display),
                description = stringResource(R.string.notification_display_desc),
                showDivider = false,
                height = 68.dp,
                onClick = {
                    navController.navigate(SettingsRoute.NotificationDisplaySetting.route)
                }
            )
            WeSettingGroup(stringResource(R.string.notification_group_sound)) {
                WeSettingItem(
                    label = stringResource(R.string.notification_msg),
                    onClick = {
                        context.navigateToAppSettings(true)
                    }
                ) {
                    WeSettingValue(stringResource(R.string.notification_system_settings))
                }
                WeSettingItem(
                    label = stringResource(R.string.notification_call),
                    onClick = {
                        context.navigateToAppSettings(true)
                    }
                ) {
                    WeSettingValue(stringResource(R.string.notification_system_settings))
                }
                WeSettingItem(
                    label = stringResource(R.string.notification_in_chat),
                    showDivider = false,
                    onClick = {
                        navController.navigate(SettingsRoute.InChatNotificationSetting.route)
                    }
                )
            }
            WeSettingGroup(stringResource(R.string.notification_group_ringtone)) {
                WeSettingItem(
                    label = stringResource(R.string.notification_msg_sound),
                    onClick = {
                        navController.navigate(SettingsRoute.NotificationSoundSetting.route)
                    }
                ) {
                    WeSettingValue("跟随系统")
                }
                WeSettingItem(
                    label = stringResource(R.string.notification_ringtone),
                    onClick = {}
                ) {
                    WeSettingValue("Lullaby")
                }
                WeSettingItem(
                    label = stringResource(R.string.notification_ringtone_hint),
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