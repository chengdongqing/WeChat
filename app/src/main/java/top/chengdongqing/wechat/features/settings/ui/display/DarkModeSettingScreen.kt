package top.chengdongqing.wechat.features.settings.ui.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.radio.WeRadioGroup
import top.chengdongqing.wechat.core.designsystem.components.switch.WeSwitch
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.settings.domain.model.AppTheme

@Composable
fun DarkModeSettingScreen(onBack: () -> Unit) {
    val themeOptions = remember {
        AppTheme.entries.filter {
            !it.isFollowSystem
        }.map {
            it.label to it
        }
    }
    var theme by remember { mutableStateOf(AppTheme.FollowSystem) }

    Scaffold(
        topBar = {
            WeTopBar(title = "深色模式", onBack = onBack) {
                WeButton(text = "完成", size = ButtonSize.Small, enabled = false)
            }
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeSettingItem(
                label = "跟随系统",
                description = "开启后，将跟随系统打开或关闭深色模式",
                showArrow = false,
                showDivider = false,
                height = 68.dp
            ) {
                WeSwitch(checked = theme.isFollowSystem) { checked ->
                    theme = if (checked) AppTheme.FollowSystem else AppTheme.Light
                }
            }

            if (!theme.isFollowSystem) {
                WeSettingGroup("手动选择") {
                    WeRadioGroup(
                        options = themeOptions,
                        value = theme
                    ) {
                        theme = it
                    }
                }
            }
        }
    }
}