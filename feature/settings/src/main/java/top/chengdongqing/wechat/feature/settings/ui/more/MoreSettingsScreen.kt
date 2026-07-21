package top.chengdongqing.wechat.feature.settings.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import top.chengdongqing.wechat.core.common.navigation.NavigationKey
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun MoreSettingsScreen(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            WeTopBar(
                title = stringResource(R.string.settings_more),
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
            WeSettingGroup {
                WeSettingItem(
                    label = stringResource(R.string.more_minor_mode),
                    onClick = {}
                )
                WeSettingItem(
                    label = stringResource(R.string.more_care_mode),
                    showDivider = false,
                    onClick = {}
                )
            }
            WeSettingGroup {
                WeSettingItem(
                    label = stringResource(R.string.more_auto_save_photo),
                    showArrow = false
                ) {
                    WeSwitch()
                }
                WeSettingItem(
                    label = stringResource(R.string.more_auto_save_video),
                    showArrow = false,
                    showDivider = false
                ) {
                    WeSwitch()
                }
            }
            WeSettingItem(
                label = stringResource(R.string.more_system_permissions),
                showDivider = false,
                onClick = {
                    backStack.add(NavigationKey.SystemPermission)
                }
            )
        }
    }
}