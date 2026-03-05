package top.chengdongqing.wechat.features.settings.ui.privacy

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
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
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
            WeSettingItem(
                label = "加我为朋友时需要验证",
                showArrow = false,
                showDivider = false
            ) {
                WeSwitch(checked = true)
            }
            WeSettingItem(
                label = "添加我的方式",
                showDivider = false,
                onClick = {
                    navController.navigate(SettingsRoute.AddMeMethodSetting.route)
                }
            )
            WeSettingGroup("朋友权限") {
                WeSettingItem(
                    label = "仅聊天",
                    onClick = {}
                )
                WeSettingItem(
                    label = "朋友圈",
                    onClick = {}
                )
                WeSettingItem(
                    label = "视频号",
                    onClick = {}
                )
                WeSettingItem(
                    label = "看一看",
                    onClick = {}
                )
                WeSettingItem(
                    label = "微信运动",
                    showDivider = false,
                    onClick = {}
                )
            }
            WeSettingItem(
                label = "通讯录黑名单",
                showDivider = false,
                onClick = {
                    navController.navigate(SettingsRoute.ContactBlacklist.route)
                }
            )
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}