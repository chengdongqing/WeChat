package top.chengdongqing.wechat.features.settings.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.settings.navigation.SettingsRoute

@Composable
fun MoreSettingsScreen(
    navController: NavHostController,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            WeTopBar(title = "更多", onBack = onBack)
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
                    label = "未成年模式",
                    onClick = {}
                )
                WeSettingItem(
                    label = "关怀模式",
                    showDivider = false,
                    onClick = {}
                )
            }
            WeSettingGroup {
                WeSettingItem(
                    label = "自动保存拍摄或编辑后的图片",
                    showArrow = false
                ) {
                    WeSwitch()
                }
                WeSettingItem(
                    label = "自动保存拍摄或编辑后的视频",
                    showArrow = false,
                    showDivider = false
                ) {
                    WeSwitch()
                }
            }
            WeSettingItem(
                label = "系统权限",
                showDivider = false,
                onClick = {
                    navController.navigate(SettingsRoute.SystemPermissionSettings.route)
                }
            )
        }
    }
}