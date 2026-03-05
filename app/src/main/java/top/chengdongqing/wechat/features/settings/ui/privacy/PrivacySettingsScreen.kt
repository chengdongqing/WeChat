package top.chengdongqing.wechat.features.settings.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.features.settings.navigation.SettingsRoute

@Composable
fun PrivacySettingsScreen(
    navController: NavHostController,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            WeTopBar(title = "朋友权限", onBack = onBack)
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
            SettingItem(
                label = "加我为朋友时需要验证",
                showArrow = false,
                showDivider = false
            ) {
                WeSwitch(checked = true)
            }
            SettingItem(
                label = "添加我的方式",
                showDivider = false,
                onClick = {
                    navController.navigate(SettingsRoute.AddMeMethodSetting.route)
                }
            )
            SettingGroup("朋友权限") {
                SettingItem(
                    label = "仅聊天",
                    onClick = {}
                )
                SettingItem(
                    label = "朋友圈",
                    onClick = {}
                )
                SettingItem(
                    label = "视频号",
                    onClick = {}
                )
                SettingItem(
                    label = "看一看",
                    onClick = {}
                )
                SettingItem(
                    label = "微信运动",
                    showDivider = false,
                    onClick = {}
                )
            }
            SettingItem(
                label = "通讯录黑名单",
                showDivider = false,
                onClick = {
                    navController.navigate(SettingsRoute.ContactBlacklist.route)
                }
            )
        }
    }
}