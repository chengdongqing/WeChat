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
import top.chengdongqing.wechat.core.designsystem.components.menu.DangerButton
import top.chengdongqing.wechat.core.designsystem.components.menu.SettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.SettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.SettingValue
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
            SettingGroup("通用") {
                SettingItem(
                    label = "通知",
                    onClick = {
                        navController.navigate(SettingsRoute.NotificationSettings.route)
                    }
                )
                SettingItem(
                    label = "界面与显示",
                    onClick = {
                        navController.navigate(SettingsRoute.DisplaySettings.route)
                    }
                )
                SettingItem(
                    label = "朋友权限",
                    onClick = {}
                )
                SettingItem(
                    label = "存储空间",
                    onClick = {}
                )
                SettingItem(
                    label = "更多",
                    showDivider = false,
                    onClick = {}
                )
            }
            SettingGroup("功能") {
                SettingItem(
                    label = "聊天",
                    onClick = {}
                )
                SettingItem(
                    label = "聊天记录管理",
                    showDivider = false,
                    onClick = {}
                )
            }
            SettingGroup("帮助与关于") {
                SettingItem(
                    label = "帮助与反馈",
                    onClick = {}
                )
                SettingItem(
                    label = "关于微信",
                    showDivider = false,
                    onClick = {}
                ) {
                    SettingValue("版本 $versionName")
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

    DangerButton(label = "退出登录", onClick = showDialog)
}