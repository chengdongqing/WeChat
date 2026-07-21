package top.chengdongqing.wechat.feature.settings.ui.notification

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.common.navigation.NavigationKey
import top.chengdongqing.wechat.core.common.util.navigateToAppSettings
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingValue
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect

@Composable
fun NotificationSettingsScreen(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val msgEnabled by viewModel.msgNotificationEnabled.collectAsStateWithLifecycle()
    val callEnabled by viewModel.callNotificationEnabled.collectAsStateWithLifecycle()
    val notificationDisplay by viewModel.notificationDisplay.collectAsStateWithLifecycle()
    val notificationSound by viewModel.notificationSound.collectAsStateWithLifecycle()
    val ringtone by viewModel.ringtone.collectAsStateWithLifecycle()
    val ringtoneAudibleEnabled by viewModel.ringtoneAudibleEnabled.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            WeTopBar(
                title = stringResource(R.string.settings_notifications),
                onBack = onBack
            )
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
                    WeSwitch(
                        checked = msgEnabled,
                        onChange = viewModel::toggleMsgNotification
                    )
                }
                WeSettingItem(
                    label = stringResource(R.string.notification_call),
                    showArrow = false,
                    showDivider = false
                ) {
                    WeSwitch(
                        checked = callEnabled,
                        onChange = viewModel::toggleCallNotification
                    )
                }
            }
            WeSettingItem(
                label = stringResource(R.string.notification_display),
                description = stringResource(notificationDisplay.descriptionRes),
                showDivider = false,
                height = 68.dp,
                onClick = {
                    backStack.add(NavigationKey.NotificationDisplaySettings)
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
                        backStack.add(NavigationKey.InChatNotificationSettings)
                    }
                )
            }
            WeSettingGroup(stringResource(R.string.notification_group_ringtone)) {
                WeSettingItem(
                    label = stringResource(R.string.notification_msg_sound),
                    onClick = {
                        backStack.add(NavigationKey.NotificationSoundSettings)
                    }
                ) {
                    WeSettingValue(stringResource(notificationSound.labelRes))
                }
                WeSettingItem(
                    label = stringResource(R.string.notification_ringtone),
                    onClick = {
                        backStack.add(NavigationKey.RingtoneSettings)
                    }
                ) {
                    WeSettingValue(stringResource(ringtone.labelRes))
                }
                WeSettingItem(
                    label = stringResource(R.string.notification_ringtone_hint),
                    showArrow = false,
                    showDivider = false
                ) {
                    WeSwitch(
                        checked = ringtoneAudibleEnabled,
                        onChange = viewModel::toggleRingtoneAudible
                    )
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}