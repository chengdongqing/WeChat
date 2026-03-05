package top.chengdongqing.wechat.features.settings.ui.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import top.chengdongqing.wechat.core.designsystem.components.menu.SettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.SettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.SettingValue
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.settings.navigation.SettingsRoute

@Composable
fun DisplaySettingsScreen(
    navController: NavHostController,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            WeTopBar(title = "界面与显示", onBack = onBack)
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingItem(
                label = "深色模式",
                showDivider = false,
                onClick = {
                    navController.navigate(SettingsRoute.ThemeSetting.route)
                }
            ) {
                SettingValue("跟随系统")
            }
            SettingGroup {
                SettingItem(
                    label = "字体大小",
                    onClick = {
                        navController.navigate(SettingsRoute.FontSizeSetting.route)
                    }
                )
                SettingItem(
                    label = "多语言",
                    showDivider = false,
                    onClick = {
                        navController.navigate(SettingsRoute.LanguageSetting.route)
                    }
                ) {
                    SettingValue("跟随系统")
                }
            }
        }
    }
}