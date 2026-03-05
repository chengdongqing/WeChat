package top.chengdongqing.wechat.features.settings.ui

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.menu.WeDangerButton
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingValue
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.util.getVersionName
import top.chengdongqing.wechat.features.settings.navigation.SettingsRoute

@Composable
fun SettingsScreen(navController: NavHostController, onBack: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember { context.getVersionName() }

    Scaffold(
        topBar = {
            WeTopBar(title = "设置", onBack = onBack)
        },
        containerColor = WeTheme.colorScheme.background,
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
            WeSettingGroup("通用") {
                WeSettingItem(
                    label = "通知",
                    onClick = {
                        navController.navigate(SettingsRoute.NotificationSettings.route)
                    }
                )
                WeSettingItem(
                    label = "界面与显示",
                    onClick = {
                        navController.navigate(SettingsRoute.DisplaySettings.route)
                    }
                )
                WeSettingItem(
                    label = "朋友权限",
                    onClick = {
                        navController.navigate(SettingsRoute.PrivacySettings.route)
                    }
                )
                WeSettingItem(
                    label = "存储空间",
                    onClick = {}
                )
                WeSettingItem(
                    label = "更多",
                    showDivider = false,
                    onClick = {
                        navController.navigate(SettingsRoute.MoreSettings.route)
                    }
                )
            }
            WeSettingGroup("功能") {
                WeSettingItem(
                    label = "聊天",
                    onClick = {}
                )
                WeSettingItem(
                    label = "聊天记录管理",
                    showDivider = false,
                    onClick = {}
                )
            }
            WeSettingGroup("帮助与关于") {
                WeSettingItem(
                    label = "帮助与反馈",
                    onClick = {}
                )
                WeSettingItem(
                    label = "关于微信",
                    showDivider = false,
                    onClick = {}
                ) {
                    WeSettingValue("版本 $versionName")
                }
            }
            LogoutButton()
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun LogoutButton() {
    val dialog = rememberDialogState()

    val showDialog = {
        dialog.show(
            title = "确定退出登录吗？",
            content = "将删除所有的数据，并彻底注销账号！",
            okColor = Danger,
            onOk = {}
        )
    }

    WeDangerButton(label = "退出登录", onClick = showDialog)
}